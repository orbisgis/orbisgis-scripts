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

class groovy_generic_function{
	def executeWorkflow(String configFileWorkflowPath, String pathCitiesToTreat, String outputFolder, String data, String[] indicatorUse, String dbUrl, String dbId, String dbPassword, Integer resetDataset, String optionalinputFilePrefix){
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
		def resultsGeoclimate=outputFolder
		File[] citiesAlreadyDone = new File(resultsGeoclimate).listFiles()

		// If 'resetDataset'=0, remove cities already done from the list to be done
		def listToBeDone = allCities
		if(resetDataset == 0){
			for (pathCity in citiesAlreadyDone){
				def city = pathCity.toString().split("/").last()[data.size()+1..-1]
				if((new File(pathCity.path+optionalinputFilePrefix)).exists()){
					listToBeDone-=city
				}
			}
		}

		if(listToBeDone.size()==0){
			println "$data worflow: no new city need to be calculated"
		}
		else{
			println "$data worflow: the following cities will be calculated:"
			println Arrays.toString(listToBeDone)

			// Create the config file
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
											"folder":"/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/BDTOPO_V2"
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

			// Reset to default the informations to connect to the Paendora DB
			if(data=="BDTOPO_V2"){
				worflow_parameters["input"]["database"]["user"]="default"
				worflow_parameters["input"]["database"]["user"]="password"
				worflow_parameters["input"]["database"]["user"]="url"
			}
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


	def unionCities(String inputDirectory, String optionalinputFilePrefix, String outputFilePathAndName, String dataset, Map thresholdColumn, String var2Model, String[] columnsToKeep, Map correspondenceTable){
		/**
		* Prepare and launch the Workflow (OSM or BDTOPO_V2) configuration file
		* @param inputDirectory 		Path where the city files are stored (actually the parent directory of the folder containing the cities)
		* @param optionalinputFilePrefix 	String to add at the end of the inputDirectory to get the right file (for example "/rsu_lcz.geojson" for the LCZ of BDTOPO_V2)
		* @param outputFilePathAndName		Path where the resulting table containing all cities will be saved
		* @param dataset 			The type of dataset that need to be unioned (OSM or BDTOPO_V2)
		* @param thresholdColumn		Map containing as key a field name and as value a threshold value below which data will be removed				
		* @param var2Model 			The name of the variable to model
		* @param columnsToKeep			List of columns to keep (except the 'varToModel' which is automatically added)
		* @param correspondenceTable		Map for converting the 'var2Model' values to a new set of values
		*
		* @return 				None
		*/
		def nbTableUnion = 100
		def queryPartialGather = ""	
		def code
		def ratio

		H2GIS datasource = H2GIS.open("${System.getProperty("java.io.tmpdir")+File.separator}cityUnion;AUTO_SERVER=TRUE", "sa", "")

		// Keep only the 'var2Model' column if columnsToKeep is empty
		def allVar2Keep = " $var2Model "
		if (columnsToKeep){
			allVar2Keep += ", ${columnsToKeep.join(", ")}"
		}

		//
		def queryThresh = ""
		if(thresholdColumn){
			queryThresh = ", ${thresholdColumn.keySet()[0]}"
		}


		File[] processedAreaList = new File(inputDirectory+dataset).listFiles()
		def i = 1
		for (areaPath in processedAreaList){	
			if ((i%nbTableUnion)==0){
				ratio = i/nbTableUnion
				datasource.execute """DROP TABLE IF EXISTS ALL_CITIES${ratio.trunc(0)}; CREATE TABLE ALL_CITIES${ratio.trunc(0)} AS ${queryPartialGather[0..-11]}"""
				queryPartialGather = ""
			}
			if(dataset == "OSM"){
				def fileName = areaPath.toString().split("/").last()
				code = fileName[4..-1]
				println "Load : '${code}', $i ème table"

				// Load RSU indicators
				datasource.load("$areaPath", "city$i", true)
			}
			if(dataset == "BDTOPO_V2"){
				def folderName = areaPath.toString().split("/").last()
				code = folderName[-6..-1]
				println "Load : '${code}', $i ème table"

				// Load RSU indicators
				datasource.load("$areaPath$optionalinputFilePrefix", "city$i", true)
			}	

			queryPartialGather += " SELECT $allVar2Keep,  FROM city$i UNION ALL "
			i+=1
		}
		ratio = (i/nbTableUnion).trunc(0)+1
		datasource.execute """DROP TABLE IF EXISTS ALL_CITIES${ratio}; CREATE TABLE ALL_CITIES${ratio} AS ${queryPartialGather[0..-11]}"""

		def tab_nb = []
		i = 1
		while (i<=ratio){
			tab_nb.add(i)
			i++
		}

		def queryGather = ""
		datasource.execute """DROP TABLE IF EXISTS ALL_CITIES_WITH_THRESHOLD; 
					CREATE TABLE ALL_CITIES_WITH_THRESHOLD 
						AS SELECT $allVar2Keep $queryThresh 
						FROM ALL_CITIES${tab_nb.join(" UNION ALL SELECT $allVar2Keep $queryThresh FROM ALL_CITIES")};
					DROP TABLE IF EXISTS ALL_CITIES;"""
		if(thresholdColumn){		
			datasource.execute """  CREATE INDEX IF NOT EXISTS id_thresh ON ALL_CITIES_WITH_THRESHOLD USING BTREE(${thresholdColumn.keySet()[0]});
						CREATE TABLE ALL_CITIES 
							AS SELECT $allVar2Keep
							FROM ALL_CITIES_WITH_THRESHOLD
							WHERE ${thresholdColumn.keySet()[0]} > ${thresholdColumn.values()[0]};"""
		}
		else{
			datasource.execute """ ALTER TABLE ALL_CITIES_WITH_THRESHOLD RENAME TO ALL_CITIES """
		}
		// The result will be saved as a geojson, thus need a geometry column
		def queryGeom = ""
		if(!columnsToKeep.contains("the_geom") && !columnsToKeep.contains("THE_GEOM")){
			datasource.execute """ALTER TABLE ALL_CITIES ADD COLUMN the_geom GEOMETRY;"""
			queryGeom = ", the_geom"
		}

		// Modify the LCZ values (String to Integer...)
		def queryModifyValues 		
		if(correspondenceTable){
			queryModifyValues = """  CREATE INDEX IF NOT EXISTS id_lcz ON ALL_CITIES USING BTREE($var2Model);
							DROP TABLE IF EXISTS ALL_CITIES_INT;
							CREATE TABLE ALL_CITIES_INT
								AS SELECT CAST("""
			def endQuery = ""			
			correspondenceTable.each{ini, fin ->
				queryModifyValues += "CASE WHEN $var2Model='$ini' THEN '$fin' ELSE "
				endQuery += " END "
			}
			queryModifyValues+= """ null $endQuery AS INTEGER) AS $var2Model $queryGeom, ${columnsToKeep.join(',')} FROM ALL_CITIES;"""
		}
		else{
			queryModifyValues = """ ALTER TABLE ALL_CITIES RENAME TO ALL_CITIES_INT;"""
		}

		datasource.execute queryModifyValues

		datasource.execute """ CALL GEOJSONWRITE('${outputFilePathAndName}', 'ALL_CITIES_INT')"""
	}
}

