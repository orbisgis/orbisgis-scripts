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
		def resultsGeoclimate=outputFolder
		File[] citiesAlreadyDone = new File(resultsGeoclimate).listFiles()

		// If 'resetDataset', remove cities already done from the list to be done
		def listToBeDone = allCities
		if(resetDataset != 0){
			for (pathCity in citiesAlreadyDone){
				def city = pathCity.toString().split("/").last()[data.size()+1..-1]
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
}

