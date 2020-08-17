// Declaration of the maven repository
@GrabResolver(name='orbisgis', root='https://nexus.orbisgis.org/repository/orbisgis/')

// Declaration of our Nexus repository, where the geoclimate project is stored
@Grab(group='org.orbisgis.orbisprocess', module='geoclimate', version='1.0.0-SNAPSHOT', classifier='jar-with-dependencies', transitive=false)

//JSON lib
@Grab(group='org.codehaus.groovy', module='groovy-json', version='3.0.4')

import org.orbisgis.orbisprocess.geoclimate.Geoclimate
import org.orbisgis.orbisprocess.geoclimate.geoindicators.Geoindicators
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS
import org.orbisgis.orbisdata.datamanager.jdbc.*
import groovy.json.JsonOutput

import groovy_generic_function

ggf = new groovy_generic_function()

// ##############################################################
// ################### MAIN SCRIPT  #############################
// ##############################################################

/////////////////////////////////////////////////////////////////
// Define needed variables //////////////////////////////////////
/////////////////////////////////////////////////////////////////
// Each argument is recovered in a variable understandable...
def configFileWorkflowPath = this.args[0]
def pathCitiesToTreat = configFileWorkflowPath + File.separator + this.args[1]
def outputFolder = this.args[2]
def data = this.args[3].toUpperCase()
def indicatorUse = this.args[4].replaceAll(" ", "").split(",")
def dbUrl = this.args[5]
def dbId = this.args[6]
def dbPassword = this.args[7]
def operationsToApply = this.args[8].replaceAll(" ", "").split(",")
def dependentVariablePath = this.args[9]
def dependentVariableColName = this.args[10]
def geometryField = this.args[11]
def sridDependentVarIndic = this.args[12]
def pathToSaveTrainingDataSet = this.args[13]
def correspondenceTable = this.args[14]
def correspondenceValMap = [:]
if(correspondenceTable){
	correspondenceTable = correspondenceTable.replaceAll(" ", "").split(",")
	// Convert the list into a Map to have the key and corresponding value
	correspondenceTable.each{	
		def keyAndVal = it.split(":")
		correspondenceValMap[keyAndVal[0]] = keyAndVal[1]
	}
}
def dependentVariable2ndColNameAndVal = this.args[15]
Integer resetDataset = this.args[16].toInteger()
def scaleTrainingDataset = this.args[17]
def classif = Boolean.valueOf(this.args[18])
def optionalinputFilePrefixTrueVal = this.args[19]


// Define some table names
def dependentVarTableName = "dependentVarTableName"
def dependentVarTableNameRaw = "dependentVarTableName_raw"

////////////////////////////////////////////////////////////////////
// Start to operate processes //////////////////////////////////////
////////////////////////////////////////////////////////////////////
// Prepare and launch the workflow
ggf.executeWorkflow(configFileWorkflowPath, pathCitiesToTreat, outputFolder, data, indicatorUse, dbUrl, dbId, dbPassword, resetDataset, optionalinputFilePrefixTrueVal)

// Open a database used for calculation and load the IAUIdF file (LCZ)
H2GIS datasource = H2GIS.open("/tmp/classification${ggf.getUuid()};AUTO_SERVER=TRUE", "sa", "")

datasource.load(dependentVariablePath, dependentVarTableNameRaw)

// Set the Srid of the independent variables dataset according to the dataset origin
def sridIndependentVarIndic
if(data == "OSM"){
	sridIndependentVarIndic = 32631
}
else if(data == "BDTOPO_V2"){
	sridIndependentVarIndic = 2154
}

// Keep the 2nd potential value for the var2Model if filled
def var2Model2Query = ""
if(dependentVariable2ndColNameAndVal!="default"){
	var2Model2Query = ", ${dependentVariable2ndColNameAndVal.split('=').first()}"
}

// Change the SRID of the input data and reduce the precision to make work the intersections
datasource.execute """ DROP TABLE IF EXISTS $dependentVarTableName;
			CREATE TABLE $dependentVarTableName 
				AS SELECT ST_PRECISIONREDUCER(ST_TRANSFORM(ST_SETSRID(ST_FORCE2D($geometryField),$sridDependentVarIndic), $sridIndependentVarIndic),2) AS the_geom, $dependentVariableColName $var2Model2Query
				FROM $dependentVarTableNameRaw;
			DROP TABLE IF EXISTS $dependentVarTableNameRaw"""
datasource.getSpatialTable(dependentVarTableName).the_geom.createIndex()
datasource.getSpatialTable(dependentVarTableName)."$dependentVariableColName".createIndex()

// For each city, gather indicators from different scales (building, block, RSU) into a single table at the scale of the training dataset
createTrainingDataset(datasource, pathCitiesToTreat, outputFolder, data, operationsToApply, dependentVarTableName, dependentVariableColName, pathToSaveTrainingDataSet, correspondenceValMap, dependentVariable2ndColNameAndVal, resetDataset, scaleTrainingDataset, classif)

// Gather the training dataset of each city into a single table

// #####################################################################
// ################### FUNCTIONS TO USE  ###############################
// #####################################################################
def createTrainingDataset(JdbcDataSource datasource, String pathCitiesToTreat, String outputFolder, String data, String[] operationsToApply, String dependentVarTableName, String dependentVariableColName, String pathToSaveTrainingDataSet, def correspondenceValMap, String dependentVariable2ndColNameAndVal, Integer resetDataset, String scaleTrainingDataset, Boolean classif){
	/**
	* For each city, gather indicators from different scales (building, block, RSU) into a single table at RSU scale and then associate the dependent variable value for the corresponding location
	* @param configFileWorkflowPath 	The path of the config file
	* @param pathCitiesToTreat		Path of the file where are stored the cities to process
	* @param outputFolder 			Path where are saved all intermediate and output results
	* @param data				The type of dataset used for the calculation of independent variables (OSM or BDTOPO_V2)
	* @param indicatorUse 			A given batch of indicators will be calculated depending on what is (are) the indicator use(s)
	* @param dbUrl				The URL of the database to download the data (in case the dataset is not OSM)
	* @param dbId 				The username (or id) for the database access
	* @param dbPassword			The password for the database access
	* @return				None
	*/
	// # The Dependent variable may have several possible values by order of priority. Create a SQL condition to keep in the training data only where the second possible typology is null (when there is only one possible value for the training)
	
	def condition2ndVal
	def idName
	if(dependentVariable2ndColNameAndVal=="default"){
		condition2ndVal = ""
	}
	else{
		condition2ndVal = "${dependentVariable2ndColNameAndVal.split("=").first()} = ${dependentVariable2ndColNameAndVal.split("=").last()} AND"
	}

	// Define table names
	def rsuIndicTempo = "rsu_indic_tempo"
	def blockIndicTempo = "block_indic_tempo"
	def buildingIndicTempo = "building_indic_tempo"
	def distributionTableBuff = "distribution_table_buff"
	def distributionTable ="distribution_table"
	def scale1ScaleFin = "scale1_scale_fin"
	def allFinalScaleIndic = "all_rsu_indic"
	def trainingData = "training_data"

	// Define the prefix used before each value of the typology (to insure that it will be string value)
	def prefixDependentVar = "VAL"

	// List of columns to remove from the analysis in building and block tables
	def buildColToRemove = ["THE_GEOM", "ID_RSU", "ID_BUILD", "ID_BLOCK", "NB_LEV", "ZINDEX", "MAIN_USE", "TYPE", "ID_SOURCE"]
	def blockColToRemove = ["THE_GEOM", "ID_RSU", "ID_BLOCK", "MAIN_BUILDING_DIRECTION"]

	// The  results will be saved in a specific folder depending on dataset type
	outputFolder += data	
	// Load in a list all cities which need to be processed
	File[] processedAreaList = new File(outputFolder).listFiles();
	for (areaPath in processedAreaList){
	    	def folderName = areaPath.toString().split("/").last()
		def areaName = folderName[data.size()+1..-1].replace("é", "e")
		// Define the name of the file where will be stored the data (in .geojson and .csv)
		def outputFilePath = pathToSaveTrainingDataSet+folderName.replace(" ", "_").replace("'", "_")
		
		if((new File(outputFilePath+".csv")).exists() && (new File(outputFilePath+".geojson")).exists() && resetDataset != 0){
			println "'$areaName' is not recalculated since '$outputFilePath' already exists and 'resetDataset' is set to 0"
		}
		else{
			println "Cross the training data with : '$areaName'"

			// Load RSU indicators
			datasource.load("$areaPath/rsu_indicators.geojson", rsuIndicTempo, true)

			// Load building indicators
			datasource.load("$areaPath/building_indicators.geojson", buildingIndicTempo, true)

			// Load block indicators
			datasource.load("$areaPath/block_indicators.geojson", blockIndicTempo, true)

			// Define default name if the 'scaleTrainingDataset' is 'RSU'	
			def finalScaleTableName = "buildingFinalTableBeforeJoin"
			def blockIndicFinalScale = blockIndicTempo
			def idScale1ForMerge
			def listblockFinalRename = []

			// Calculate average and variance at RSU scale from each indicator of the building scale
			def inputVarAndOperationsBuild = [:]
			def buildIndicators = datasource.getTable(buildingIndicTempo).getColumns()
			for (col in buildIndicators){
				if (!buildColToRemove.contains(col)){
					inputVarAndOperationsBuild[col] = operationsToApply
				}
			}
			def calcBuildStat = Geoindicators.GenericIndicators.unweightedOperationFromLowerScale()
		    	calcBuildStat.execute([	inputLowerScaleTableName: buildingIndicTempo,
						inputUpperScaleTableName: rsuIndicTempo,
		       				inputIdUp: "id_rsu", inputIdLow: "id_build", 
		       				inputVarAndOperations: inputVarAndOperationsBuild,
		       				prefixName: "bu", datasource: datasource])
		   	def buildIndicRsuScale = calcBuildStat.results.outputTableName

			// To avoid crashes of the join due to column duplicate, need to prefix some names
			def buildRsuCol2Rename = datasource.getTable(buildIndicRsuScale).getColumns()
			def listBuildRsuRename = []
			for (col in buildRsuCol2Rename){
				if(col != "ID_BUILD" && col != "ID_BLOCK" && col != "ID_BLOCK" && col != "THE_GEOM"){
					listBuildRsuRename.add("a.$col AS build_$col")
				}
			}

			// Special processes if the scale of analysis is RSU
			if(scaleTrainingDataset == "RSU"){
				// Calculate building average and variance at RSU scale from each indicator of the block scale
				def inputVarAndOperationsBlock = [:]
				def blockIndicators = datasource.getTable(blockIndicTempo).getColumns()
				for (col in blockIndicators){
					if (!blockColToRemove.contains(col)){
						inputVarAndOperationsBlock[col] = operationsToApply
					}
				}
				// Calculate block indicators averaged at RSU scale
				def calcBlockStat = Geoindicators.GenericIndicators.unweightedOperationFromLowerScale()
			    	calcBlockStat.execute([	inputLowerScaleTableName: blockIndicTempo,
							inputUpperScaleTableName: rsuIndicTempo,
			       				inputIdUp: "id_rsu", inputIdLow: "id_block", 
				       			inputVarAndOperations: inputVarAndOperationsBlock,
				       			prefixName: "bl", datasource: datasource])
				blockIndicFinalScale = calcBlockStat.results.outputTableName
				
				// To avoid crashes of the join due to column duplicate, need to prefix some names
				def blockRsuCol2Rename = datasource.getTable(blockIndicFinalScale).getColumns()				
				listblockFinalRename = []
				for (col in blockRsuCol2Rename){
					if(col != "ID_BLOCK" && col != "ID_RSU" && col != "THE_GEOM"){
						listblockFinalRename.add("b.$col AS block_$col")
					}
				}				

				// Define generic name whatever be the 'scaleTrainingDataset'	
				finalScaleTableName = rsuIndicTempo
				// Useful for merge between buildings and rsu tables
				idbuildForMerge = "id_rsu"
				idBlockForMerge = "id_block"
				// Useful if the classif is a regression
				idName = "id_rsu"
			}

			// Special processes if the scale of analysis is building
			else if(scaleTrainingDataset ==  "BUILDING"){			
				// Need to join RSU and building tables
				def listRsuCol = datasource.getTable(rsuIndicTempo).getColumns()
				listRsuRename = []
				for (col in listRsuCol){
					if(col != "ID_RSU" && col != "THE_GEOM"){
						listRsuRename.add("a.$col AS rsu_$col")
					}
				}
				def listBuildCol = datasource.getTable(buildingIndicTempo).getColumns()
				def listBuildRename = []
				for (col in listBuildCol){
					if(col != "ID_RSU" && col != "ID_BLOCK" && col != "ID_BUILD" && col != "THE_GEOM"){
						listBuildRename.add("b.$col AS build_$col")
					}
					else{
						listBuildRename.add("b.$col")
					}
				}
				
				// Merge scales (building and Rsu indicators)
				datasource.getTable(rsuIndicTempo).id_rsu.createIndex()
				datasource.getTable(buildingIndicTempo).id_rsu.createIndex()
				datasource.execute """ DROP TABLE IF EXISTS $finalScaleTableName;
							CREATE TABLE $finalScaleTableName 
								AS SELECT ${listRsuRename.join(', ')}, ${listBuildRename.join(', ')} 
								FROM $rsuIndicTempo a RIGHT JOIN $buildingIndicTempo b
								ON a.id_rsu = b.id_rsu;"""

				// To avoid crashes of the join due to column duplicate, need to prefix some names		
				def blockCol2Rename = datasource.getTable(blockIndicTempo).getColumns()				
				for (col in blockCol2Rename){
					if(col != "ID_BLOCK" && col != "ID_RSU" && col != "THE_GEOM"){
						listblockFinalRename.add("b.$col AS block_$col")
					}
				}
				// Useful for merge between gathered building and rsu indicators and building indicators averaged at RSU scale
				idbuildForMerge = "id_rsu"
				idBlockForMerge = "id_block"
				// Useful if the classif is a regression				
				idName = "id_build"
			}

			// Gather all indicators (coming from three different scales) in a single table (the 'scaleTrainingDataset' scale)
			// Note that in order to avoid crashes of the join due to column duplicate, indicators are prefixed
			datasource.getTable(buildIndicRsuScale).id_rsu.createIndex()
			datasource.getTable(finalScaleTableName).id_rsu.createIndex()
			datasource.execute """ DROP TABLE IF EXISTS $scale1ScaleFin;
						CREATE TABLE $scale1ScaleFin 
							AS SELECT ${listBuildRsuRename.join(', ')}, b.*
							FROM $buildIndicRsuScale a RIGHT JOIN $finalScaleTableName b
							ON a.$idbuildForMerge = b.$idbuildForMerge;"""
			datasource.getTable(blockIndicFinalScale).id_rsu.createIndex()
			datasource.getTable(scale1ScaleFin).id_rsu.createIndex()
			datasource.execute """ DROP TABLE IF EXISTS $allFinalScaleIndic;
						CREATE TABLE $allFinalScaleIndic 
							AS SELECT a.*, ${listblockFinalRename.join(', ')}
							FROM $scale1ScaleFin a LEFT JOIN $blockIndicFinalScale b
							ON a.$idBlockForMerge = b.$idBlockForMerge;"""

			// If the randomForest is a classification, calculate the intersection between the training dataset and the indicator at the same scale and then create a distribution table
			if(classif){
				datasource.getSpatialTable(allFinalScaleIndic).the_geom.createIndex()
				def areaCalcQuery = "ST_AREA(ST_INTERSECTION(a.the_geom,b.the_geom))"
				def casewhenQuery = ""
				def sumQuery = ""
				def ifVarchar = ""
				// Initial 'var2Model' values may be INTEGER or VARCHAR, thus need to add 'in the queries if VARCHAR
				if(datasource."$dependentVarTableName"."$dependentVariableColName".getType() == "VARCHAR"){
					ifVarchar = "'"
				}
				correspondenceValMap.each{ind, val ->
					casewhenQuery+="CASE WHEN a.$dependentVariableColName=$ifVarchar${ind}$ifVarchar THEN ${areaCalcQuery} ELSE 0 END AS $prefixDependentVar${val}, "
					sumQuery += "SUM($prefixDependentVar${val}) AS $prefixDependentVar${val}, "
				}
				datasource.execute	"""DROP TABLE IF EXISTS $distributionTableBuff;
							CREATE TABLE $distributionTableBuff
									AS SELECT b.id_rsu, ${casewhenQuery[0..-3]}
									FROM $dependentVarTableName a, $allFinalScaleIndic b 
									WHERE $condition2ndVal a.the_geom && b.the_geom AND ST_INTERSECTS(a.the_geom, b.the_geom)"""

				datasource.getTable(distributionTableBuff).id_rsu.createIndex()

				datasource.execute """
									DROP TABLE IF EXISTS $distributionTable, distribution_repartition;
									CREATE TABLE $distributionTable
											AS SELECT id_rsu, ${sumQuery[0..-3]}
											FROM $distributionTableBuff
											GROUP BY id_rsu;"""

				// The main typology and indicators characterizing the distribution are calculated
				def computeDistribChar = Geoindicators.GenericIndicators.distributionCharacterization()
				computeDistribChar.execute([distribTableName:   distributionTable,
							    inputId:            "id_rsu",
							    distribIndicator:   ["uniqueness"],
							    extremum:           "GREATEST",
							    prefixName:         "",
							    datasource:         datasource])
				def resultsDistrib = computeDistribChar.getResults().outputTableName

				// We merge the more appropriate LCZ value to the indicators calculated using OSM data 
				// Note that we conserve the uniqueness value to be able to select only the most unique value in the training stage
				datasource.getTable(resultsDistrib).id_rsu.createIndex()
				datasource.getTable(allFinalScaleIndic).id_rsu.createIndex()
				datasource.execute """ DROP TABLE IF EXISTS $trainingData;
										CREATE TABLE $trainingData
												AS SELECT a.EXTREMUM_COL AS $dependentVariableColName, a.UNIQUENESS_VALUE, b.*
												FROM $resultsDistrib a 
													RIGHT JOIN $allFinalScaleIndic b
													ON a.id_rsu = b.id_rsu;
												"""
			}
			// If the randomForest is a regression, calculate a weighted average of the 'var2Model'
			else{
				// To avoid duplicate id name (id_build and id_build or id_rsu and id_rsu), need to rename the id from the targeted table
				datasource.execute """ ALTER TABLE $allFinalScaleIndic RENAME COLUMN $idName TO id_target"""

				// Create an id names 'idName' if not exists in the dependent table
				if(!datasource.getTable(dependentVarTableName).getColumns().contains(idName.toUpperCase())){
					datasource.execute """ ALTER TABLE $dependentVarTableName ADD COLUMN $idName INTEGER AUTO_INCREMENT;"""
				}
				
				// Create a spatial join between source and target tables	        
				def computeSpatialJoin =  Geoindicators.SpatialUnits.spatialJoin()
				computeSpatialJoin.execute([
							sourceTable     : dependentVarTableName,
							targetTable     : allFinalScaleIndic,
							idColumnTarget  : "id_target",
							pointOnSurface  : false,
							nbRelations     : null,
							prefixName      : "",
							datasource      : datasource])
				def spatialJoined = computeSpatialJoin.results.outputTableName

				datasource.execute """ ALTER TABLE $spatialJoined DROP COLUMN THE_GEOM"""

				// Calculate the area weighted value of the var2Model in the target scale (the mode would be too much complicated with continuous values) AND also an indicator of uniqueness of the intersection between buildings (what proportion of building in the target table is covered by the most intersected building in the source table ?)
				datasource.getTable(spatialJoined).id_target.createIndex()
				datasource.execute """ 	DROP TABLE IF EXISTS weighted_average;
							CREATE TABLE weighted_average
								AS SELECT 	id_target, 
										SUM(AREA*$dependentVariableColName)/SUM(AREA) AS $dependentVariableColName,
										MAX(AREA)/SUM(AREA) AS UNIQUENESS_VALUE
								FROM $spatialJoined
								GROUP BY id_target;"""

				// Create the training data (gathering dependent (area weighted) and independent variables)
			        def computeJoinFinal = Geoindicators.DataUtils.joinTables()
				computeJoinFinal.execute([
						inputTableNamesWithId   : ["weighted_average":"id_target", (allFinalScaleIndic):"id_target"],
						outputTableName         : trainingData,
						datasource              : datasource])
				def finalJoined = computeJoinFinal.results.outputTableName	

				// Reinitialize the name of the initial id (id_build) instead of id_target
				datasource.execute """ ALTER TABLE $finalJoined RENAME COLUMN id_target TO $idName"""
			}

			// Save both the file in a geojson format for spatial investigation and in csv to manipulate easily with the Pandas library of Python
			println "will save $outputFilePath"
			datasource.save(trainingData, "${outputFilePath}.geojson")
			println "saved"
			datasource.execute """ ALTER TABLE $trainingData DROP COLUMN the_geom;"""
			datasource.getTable(trainingData).reload()
			datasource.save(trainingData, "${outputFilePath}.csv")
			println("'$areaName' has been processed")
		}
	}
}

