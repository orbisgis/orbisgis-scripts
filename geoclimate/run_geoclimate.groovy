/*================================================================================
* DEPENDENCIES AND MODULES
*/
// MAVEN repository
@GrabResolver(name="orbisgis", root="https://nexus.orbisgis.org/repository/orbisgis/")

// GEOCLIMATE dependencies
@Grab(group="org.orbisgis.orbisprocess", module="geoclimate", version="1.0.0-SNAPSHOT", classifier="jar-with-dependencies", transitive=false)

// JSON dependencies
@Grab(group="org.codehaus.groovy", module="groovy-json", version="3.0.4")

// JDBC dependencies
@Grab(group="org.orbisgis.orbisdata.datamanager", module="jdbc", version="1.0.1-SNAPSHOT")

import org.orbisgis.orbisprocess.geoclimate.Geoclimate
import org.orbisgis.orbisprocess.geoclimate.geoindicators.Geoindicators
import org.orbisgis.orbisdata.datamanager.jdbc.postgis.POSTGIS
import groovy.json.JsonOutput
import org.locationtech.jts.io.WKTReader

/*================================================================================
* OUTPUT PATHS
*/
String directory = "/tmp/geoclimate_chain/"
File dirFile = new File(directory)
dirFile.delete()
dirFile.mkdir()

String outputDirectory = "/tmp/outputs/"
File outdirFile = new File(outputDirectory)
outdirFile.delete()
outdirFile.mkdir()

String location = "database" //"folder" or "database"
def output = null

switch(location) {
    case "database":
        output = [
            "database": [ //database parameters to store the computed layers.
                "user": "postgres",
                "password": "postgres",
                "url": "jdbc:postgresql://localhost:5432/", //JDBC url to connect with the database
                "tables": [
                    "building_indicators":"building_indicators",
                    "block_indicators":"block_indicators",
                    "rsu_indicators":"rsu_indicators",
                    "rsu_lcz":"rsu_lcz",
                    "zones":"zones",
                ]
            ]
	]
	break;

    case "folder":
        output = [
            "folder" : "$outputDirectory", 
            "tables": [
                "building_indicators",
                "block_indicators",
                "rsu_indicators",
                "rsu_lcz",
                "zones",
                "building",
                "road",
                "rail" ,
                "water",
                "vegetation",
                "impervious"
            ]
        ]
        break;
}

/*================================================================================
* PARAMETERS
*/
def osm_parameters = [
         "description" :"Example of configuration file to run the OSM workflow and store the results in ${location}",
         "geoclimatedb" : [
             "path" : "${dirFile.absolutePath+File.separator+"geoclimate_db;AUTO_SERVER=TRUE"}",
             "delete" : true
         ],
         "input" : [
             "osm" : [[47.654114,-2.764907,47.661746,-2.750273]]
         ],
         "output" : output,
         "parameters": [
             "distance": 0,
             "indicatorUse": ["TEB", "LCZ"],
             "svfSimplified": false,
             "prefixName": "",
             "hLevMin": 3,
             "hLevMax": 15,
             "hThresholdLev2": 10
   	  ]
      ]

/*================================================================================
* GEOCLIMATE
*/        
def process = Geoclimate.OSM.workflow
def isValidProcess = process.execute(configurationFile:createOSMConfigFile(osm_parameters, outputDirectory))

if (isValidProcess){
    /*------------------------------------------------------------
     PostGIS database properties 
    */
    def dbProperties = [databaseName: 'postgres',
                        user        : 'postgres',
                        password    : 'postgres',
                        url         : "jdbc:postgresql://localhost:5432/"]

    def postGIS = POSTGIS.open(dbProperties)
    
    if (location == 'database') {
        postGIS.execute("DROP TABLE IF EXISTS GRID;")
    }
    
    /*------------------------------------------------------------
     Make gridded domain with 1x1 km2 cells 
     */
    def gridProcess = Geoindicators.SpatialUnits.createGrid()
    
    def wktReader = new WKTReader()
    def box = wktReader.read('POLYGON((47.654114 -2.764907, 47.654114 -2.750273, 47.661746 -2.750273, 47.661746 -2.764907, 47.654114 -2.764907))')
    box.setSRID(4326)
    gridProcess.execute(
        [geometry: box, 
         deltaX: 1, 
         deltaY: 1, 
         prefixName: "", 
         datasource: postGIS])
                         
    println('grid process ok')
    
     /*------------------------------------------------------------
     Make aggregation with previous grid and current rsu area
     */
    def targetTableName = gridProcess.results.outputTableName
    def indicatorTableName = "rsu_lcz"
    def indicatorName = "lcz1"
    
    def upperScaleAreaStatistics = Geoindicators.GenericIndicators.upperScaleAreaStatistics()
    upperScaleAreaStatistics.execute(
        [upperTableName: targetTableName,
         upperColumnId : "id",
         lowerTableName: indicatorTableName,
         lowerColumName: indicatorName,
         prefixName    : "agg",
         datasource    : postGIS])
         
    println('aggregation process ok')

} else {
    println('Geoclimate process invalid')
}
/*================================================================================
* FUNCTIONS
*/  
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
    configFile.write(JsonOutput.prettyPrint(json))
    return configFile.absolutePath
}
