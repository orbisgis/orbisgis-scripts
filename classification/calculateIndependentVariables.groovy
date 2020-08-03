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

// ##############################################################
// ################### MAIN SCRIPT  #############################
// ##############################################################

/////////////////////////////////////////////////////////////////
// Define needed variables //////////////////////////////////////
/////////////////////////////////////////////////////////////////
// Each argument is recovered in a variable understandable...
def configFileWorkflowPath = this.args[0]
def pathCitiesToTreat = this.args[1]
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
def lczValList = this.args[14].replaceAll(" ", "").split(",")
def dependentVariable2ndColNameAndVal = this.args[15]
Integer resetDataset = this.args[16]
def scaleTrainingDataset = this.args[17]

// Convert the list into a Map to have the key and corresponding value
def lczValMap = [:]
lczValList.each{
	def keyAndVal = it.split(":")
	lczValMap[keyAndVal[0]] = keyAndVal[1]
}

// Define some table names
def dependentVarTableName = "dependentVarTableName"
def dependentVarTableNameRaw = "dependentVarTableName_raw"

////////////////////////////////////////////////////////////////////
// Start to operate processes //////////////////////////////////////
////////////////////////////////////////////////////////////////////
// Prepare and launch the workflow
executeWorkflow(configFileWorkflowPath, pathCitiesToTreat, outputFolder, data, indicatorUse, dbUrl, dbId, dbPassword, resetDataset)

// Open a database used for calculation and load the IAUIdF file (LCZ)
H2GIS datasource = H2GIS.open("/tmp/classification${getUuid()};AUTO_SERVER=TRUE", "sa", "")
datasource.load(dependentVariablePath, dependentVarTableNameRaw)

// Set the Srid of the independent variables dataset according to the dataset origin
def sridIndependentVarIndic
if(data == "OSM"){
	sridIndependentVarIndic = 32631
}
else if(data == "BDTOPO_V2"){
	sridIndependentVarIndic = 2154
}

// Change the SRID of the input data and reduce the precision to make work the intersections
datasource.execute """ DROP TABLE IF EXISTS $dependentVarTableName;
			CREATE TABLE $dependentVarTableName 
				AS SELECT ST_PRECISIONREDUCER(ST_TRANSFORM(ST_SETSRID(ST_FORCE2D($geometryField),$sridDependentVarIndic), $sridIndependentVarIndic),2) AS the_geom, $dependentVariableColName, ${dependentVariable2ndColNameAndVal.split('=').first()}
				FROM $dependentVarTableNameRaw;
			DROP TABLE IF EXISTS $dependentVarTableNameRaw"""
datasource.getSpatialTable(dependentVarTableName).the_geom.createIndex()
datasource.getSpatialTable(dependentVarTableName)."$dependentVariableColName".createIndex()

// Gather indicators from different scales (building, block, RSU) into a single table at the scale of the training dataset
createTrainingDataset(datasource, pathCitiesToTreat, outputFolder, data, operationsToApply, dependentVarTableName, dependentVariableColName, pathToSaveTrainingDataSet, lczValMap, dependentVariable2ndColNameAndVal, resetDataset, scaleTrainingDataset)

// 

// #####################################################################
// ################### FUNCTIONS TO USE  ###############################
// #####################################################################
def createTrainingDataset(JdbcDataSource datasource, String pathCitiesToTreat, String outputFolder, String data, String[] operationsToApply, String dependentVarTableName, String dependentVariableColName, String pathToSaveTrainingDataSet, def lczValMap, String dependentVariable2ndColNameAndVal, Integer resetDataset, String scaleTrainingDataset){
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

			// Define generic name whatever be the 'scaleTrainingDataset'	
			def finalScaleTableName
			def scale1TableName
			def scale2TableName
			def idScale1ForMerge
			// To avoid crashes of the join due to column duplicate, need to prefix some names
			def scale1Col2Rename
			def scale2Col2Rename
			def listScal1Rename
			def listScal2Rename
			if(scaleTrainingDataset == "RSU"){
				// Calculate average and variance at RSU scale from each indicator of the building scale
				def inputVarAndOperationsBuild = [:]
				def buildIndicators = datasource.getTable(buildingIndicTempo).getColumns()
				for (col in buildIndicators){
					if (!buildColToRemove.contains(col)){
						inputVarAndOperationsBuild[col] = operationsToApply
					}
				}
				println inputVarAndOperationsBuild
				def calcBuildStat = Geoindicators.GenericIndicators.unweightedOperationFromLowerScale()
			    	calcBuildStat.execute([	inputLowerScaleTableName: buildingIndicTempo,
							inputUpperScaleTableName: rsuIndicTempo,
			       				inputIdUp: "id_rsu", inputIdLow: "id_build", 
			       				inputVarAndOperations: inputVarAndOperationsBuild,
			       				prefixName: "bu", datasource: datasource])
			   	def buildIndicRsuScale = calcBuildStat.results.outputTableName
			    
			    
				// Calculate building average and variance at RSU scale from each indicator of the building scale
				def inputVarAndOperationsBlock = [:]
				def blockIndicators = datasource.getTable(blockIndicTempo).getColumns()
				for (col in blockIndicators){
					if (!blockColToRemove.contains(col)){
						inputVarAndOperationsBlock[col] = operationsToApply
					}
				}
				def calcBlockStat = Geoindicators.GenericIndicators.unweightedOperationFromLowerScale()
			    	calcBlockStat.execute([	inputLowerScaleTableName: blockIndicTempo,
							inputUpperScaleTableName: rsuIndicTempo,
			       				inputIdUp: "id_rsu", inputIdLow: "id_block", 
				       			inputVarAndOperations: inputVarAndOperationsBlock,
				       			prefixName: "bl", datasource: datasource])
				def blockIndicRsuScale = calcBlockStat.results.outputTableName

				// To avoid crashes of the join due to column duplicate, need to prefix some names
				scale1Col2Rename = datasource.getTable(buildIndicRsuScale).getColumns()
				scale2Col2Rename = datasource.getTable(blockIndicRsuScale).getColumns()
				listScal1Rename = []
				listScal2Rename = []
				for (col in scale1Col2Rename){
					listScal1Rename.add("a.$col AS build_$col")
				}
				for (col in scale2Col2Rename){
					listScal2Rename.add("b.$col AS block_$col")
				}

				// Define generic name whatever be the 'scaleTrainingDataset'	
				finalScaleTableName = rsuIndicTempo
				scale1TableName = buildIndicRsuScale
				scale2TableName = blockIndicRsuScale
				// Useful for merge between buildings and rsu tables
				idScale1ForMerge = "id_rsu"
			}
			else if(scaleTrainingDataset ==  "BUILDING"){
				// Only need to gather indicators from block and RSU scale to building scale (to avoid crashes of the join due to column duplicate, need to prefix some names)
				scale1Col2Rename = datasource.getTable(blockIndicTempo).getColumns()
				scale2Col2Rename = datasource.getTable(rsuIndicTempo).getColumns()
				listScal1Rename = []
				listScal2Rename = []
				for (col in scale1Col2Rename){
					listScal1Rename.add("a.$col AS block_$col")
				}
				for (col in scale2Col2Rename){
					listScal2Rename.add("b.$col AS rsu_$col")
				}
				// Define generic name whatever be the 'scaleTrainingDataset'	
				finalScaleTableName = buildingIndicTempo
				scale1TableName = buildIndicRsuScale
				scale2TableName = blockIndicRsuScale
				// Useful for merge between buildings and blocks tables
				idScale1ForMerge = "id_block"
			}

			// Gather all indicators (coming from three different scales) in a single table (the 'scaleTrainingDataset' scale)
			// Note that in order to avoid crashes of the join due to column duplicate, indicators are prefixed
			datasource.getTable(scale1TableName).id_rsu.createIndex()
			datasource.getTable(finalScaleTableName).id_rsu.createIndex()
			datasource.execute """ DROP TABLE IF EXISTS $scale1ScaleFin;
						CREATE TABLE $scale1ScaleFin 
							AS SELECT ${listScal1Rename.join(', ')}, b.*
							FROM $scale1TableName a RIGHT JOIN $finalScaleTableName b
							ON a.$idScale1ForMerge = b.$idScale1ForMerge;"""
			datasource.getTable(scale2TableName).id_rsu.createIndex()
			datasource.getTable(scale1ScaleFin).id_rsu.createIndex()
			datasource.execute """ DROP TABLE IF EXISTS $allFinalScaleIndic;
						CREATE TABLE $allFinalScaleIndic 
							AS SELECT a.*, ${listScal2Rename.join(', ')}
							FROM $scale1ScaleFin a LEFT JOIN $scale2TableName b
							ON a.id_rsu = b.id_rsu;"""
			
			// Calculate the intersection between the training dataset and the indicator at the same scale and then create a distribution table
			datasource.getSpatialTable(allFinalScaleIndic).the_geom.createIndex()
			def areaCalcQuery = "ST_AREA(ST_INTERSECTION(a.the_geom,b.the_geom))"
			def casewhenQuery = ""
			def sumQuery = ""
			lczValMap.eachWithIndex{ind, val ->
				casewhenQuery+="CASE WHEN a.$dependentVariableColName='${ind}' THEN ${areaCalcQuery} ELSE 0 END AS $prefixDependentVar${val}, "
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
											AS SELECT a.EXTREMUM_COL AS LCZ, a.UNIQUENESS_VALUE, b.*
											FROM $resultsDistrib a 
												RIGHT JOIN $allFinalScaleIndic b
												ON a.id_rsu = b.id_rsu;
											"""

			// Save both the file in a geojson format for spatial investigation and in csv to manipulate easily with the Pandas library of Python
			datasource.save(trainingData, "${outputFilePath}.geojson")
			datasource.execute """ ALTER TABLE $trainingData DROP COLUMN the_geom;"""
			datasource.getTable(trainingData).reload()
			datasource.save(trainingData, "${outputFilePath}.csv")
			println("'$areaName' has been processed")
		}
	}
}


def executeWorkflow(String configFileWorkflowPath, String pathCitiesToTreat, String outputFolder, String data, String[] indicatorUse, String dbUrl, String dbId, String dbPassword, Integer resetDataset){
	/**
	* Prepare and launch the Workflow (OSM or BDTOPO_V2) configuration file
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
	// The  results are saved in a specific folder depending on dataset type
	outputFolder = outputFolder + data
	
	// Modify the input db parameters (if they have "default" as value, it means they are supposed to be "")
	if(dbUrl=="default"){
		dbUrl = ""
	}
	if(dbId=="default"){
		dbId = ""
	}
	if(dbPassword=="default"){
		dbPassword = ""
	}

	// Create the config file that will be used for the calculation of OSM Workflow
	File dirFile = new File(configFileWorkflowPath)
	dirFile.delete()
	dirFile.mkdir()

	// Load the file containing all cities that need to be done
	def line
	new File(pathCitiesToTreat).withReader('UTF-8') { reader ->
	    line = reader.readLine().replace('"', '')
	}
	def allCities = line.split(",")

	// Load cities already done
	resultsGeoclimate=outputFolder
	File[] citiesAlreadyDone = new File(resultsGeoclimate).listFiles()

	// If 'resetDataset', remove cities already done from the list to be done
	def listToBeDone = allCities
	if(resetDataset != 0){
		for (pathCity in citiesAlreadyDone){
			city = pathCity.toString().split("/").last()[data.size()+1..-1]
			listToBeDone-=city
		}
	}

	if(listToBeDone.size()==0){
		println "$data worflow: no new city need to be calculated"
	}
	else{
		println "$data worflow: the following cities will be calculated:"
		println Arrays.toString(listToBeDone)

		// Create the config file
		// Convert the 'indicatorUse' parameter to array of string

		def worflow_parameters	
		if(data=="OSM"){
			worflow_parameters = [
				    	"description" :"Apply the $data workflow",
				    	"geoclimatedb" : 		[
									"path" : "/tmp/geoclimate_db;AUTO_SERVER=TRUE",
									"delete" :true
						    			],
				    	"input" : 			[
									"osm" : listToBeDone
									],
				    	"output" :			[
				     					"folder" : outputFolder
									],
					"parameters":
								    	[
									"distance" : 1000,
									"indicatorUse": indicatorUse,
									"svfSimplified": false,
									"prefixName": "",
									"mapOfWeights":
											["sky_view_factor": 1,
											    "aspect_ratio": 1,
											    "building_surface_fraction": 1,
											    "impervious_surface_fraction" : 1,
											    "pervious_surface_fraction": 1,
											    "height_of_roughness_elements": 1,
											    "terrain_roughness_class": 1
											],
									"hLevMin": 3,
									"hLevMax": 15,
									"hThresholdLev2": 10
						    			]
					]
		}
		if(data=="BDTOPO_V2"){
			worflow_parameters = [
						"description" :"Apply the $data workflow",
						"geoclimatedb" : 		[
										"path" : "/tmp/geoclimate_db;AUTO_SERVER=TRUE",
										"delete" :true
							    			],
						"input" : 			[
										"database": [
											    "user": dbId,
											    "password": dbPassword,
											    "url": dbUrl,
											    "id_zones":	listToBeDone,
											    "tables": 	[
													"iris_ge":"ign_iris.iris_ge_2016",
													"bati_indifferencie":"ign_bdtopo_2017.bati_indifferencie",
													"bati_industriel":"ign_bdtopo_2017.bati_industriel",
													"bati_remarquable":"ign_bdtopo_2017.bati_remarquable",
														"route":"ign_bdtopo_2017.route",
													"troncon_voie_ferree":"ign_bdtopo_2017.troncon_voie_ferree",
														"surface_eau":"ign_bdtopo_2017.surface_eau",
													"zone_vegetation":"ign_bdtopo_2017.zone_vegetation",
														"terrain_sport":"ign_bdtopo_2017.terrain_sport",
													"construction_surfacique":"ign_bdtopo_2017.construction_surfacique",
														"surface_route":"ign_bdtopo_2017.surface_route",
													"surface_activite":"ign_bdtopo_2017.surface_activite"
													]
												]
										],
						"output" :			[
										"/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/BDTOPO_V2"
										],
						"parameters":			[
										"distance" : 1000,
										"indicatorUse": indicatorUse,
										"svfSimplified": false,
										"prefixName": "",
										"hLevMin": 3,
										"hLevMax": 15,
										"hThresholdLev2": 10
										]
						]
		}
		// Fill the workflow config parameter and return the path
		def configFilePath = createOSMConfigFile(worflow_parameters, configFileWorkflowPath)

		// Produce the indicators for the entire Ile de France and save results in files
		produceIndicatorsForTest(configFilePath, data)
	}
}

/**
* Execute several cities where we have testsIProcess process = ProcessingChain.Workflow.BDTOPO_V2()
* @param configFile 	The path of the config file
* @param data		The type of dataset (OSM or BDTOPO_V2)
* @return		None
*/
void produceIndicatorsForTest(String configFile, String data) {
	if (data == "OSM"){	
		def process = Geoclimate.OSM.workflow
		process.execute(configurationFile: configFile)
	}
	else if (data == "BDTOPO_V2"){
		def process = Geoclimate.BDTOPO_V2.workflow
		process.execute(configurationFile: configFile)
	}
}

/**
* Simple function to create a large number to set random filename 
* @return the number as String
*/
static def getUuid(){
    UUID.randomUUID().toString().replaceAll("-", "_") }

/**
* Create a configuration file
* @param 	osmParameters
* @param 	directory
* @return	the path of the config file name
*/
def createOSMConfigFile(def worflow_parameters, def directory){
	def json = JsonOutput.toJson(worflow_parameters)
	def configFilePath =  directory+File.separator+"configFile.json"
	File configFile = new File(configFilePath)
	if(configFile.exists()){
	    configFile.delete()
	} 
	configFile.write(json)
	return configFile.absolutePath
}

