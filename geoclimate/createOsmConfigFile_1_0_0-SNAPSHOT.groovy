/*#########################################################################################
This script allow to create a configuration file that may be used to store all parameters
useful as input of the OSM geoclimate workflow (version 1_0_0-SNAPSHOT).
###########################################################################################*/
//Create on 2020-07
//@author: Erwan Bocher (CNRS)

// Declaration of the maven repository
@GrabResolver(name='orbisgis', root='https://nexus.orbisgis.org/repository/orbisgis/')

// Declaration of our Nexus repository, where the geoclimate project is stored
@Grab(group='org.orbisgis.orbisprocess', module='geoclimate', version='1.0.0-SNAPSHOT', classifier='jar-with-dependencies', transitive=false)
//JSON lib
@Grab(group='org.codehaus.groovy', module='groovy-json', version='3.0.4')


import org.orbisgis.orbisprocess.geoclimate.Geoclimate
import groovy.json.JsonOutput


String directory ="/tmp/geoclimate_chain"
        File dirFile = new File(directory)
        dirFile.delete()
        dirFile.mkdir()
        def osm_parameters = [
                "description" :"Example of configuration file to run the OSM workflow and store the resultst in a folder",
                "geoclimatedb" : [
                        "path" : "${dirFile.absolutePath+File.separator+"geoclimate_chain_db;AUTO_SERVER=TRUE"}",
                        "delete" :true
                ],
                "input" : [
                        "osm" : ["Saint Jean La Poterie"]],
                "output" :[
                        "folder" : "$directory"]
        ]
        
def process = Geoclimate.OSM.workflow
process.execute(configurationFile:createOSMConfigFile(osm_parameters, directory))


/**
* Create a configuration file
* @param osmParameters
* @param directory
* @return
*/
def createOSMConfigFile(def osmParameters, def directory){
	def json = JsonOutput.toJson(osmParameters)
	def configFilePath =  directory+File.separator+"osmConfigFile.json"
	File configFile = new File(configFilePath)
	if(configFile.exists()){
	    configFile.delete()
	}
	configFile.write(json)
	return configFile.absolutePath
}
