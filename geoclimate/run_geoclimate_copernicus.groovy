/*
* This script is used to process the Geoclimate chain as part of the SLIM project (C3S consortium).
* It has been implemented to make statistics on urban geoindicators for several UTM zones 
* at European scale, to provide information at high spatial resolution (downscaling).
*
* Decription
* ----------
* We previously generated a regular mesh in the metric system that is built with 10x10 km² domains.
* A Land-Sea Mask filter has been applied to this mesh, that allow to keep only domains 
* with no 'in-water' grid points.
*
* 1. The Geoclimate chain is then used a first time, to extract OSM data for each of these domains 
*    which the coordinates have been provided by the user, to compute all the geoindicators.
* 2. In a second hand, we call two processes defined in the chain:
     - to create 1x1 km² grid cells inside of each domain,
     - to aggregate all the indicators for each of these cells.
*    
* Inputs
* ----------
* A list of bounding boxes coordinates corresponding to one or several domains of interest.

* Outputs
* ----------
* Data are stored as Geojson files or in database (H2GIS)
*
* @author Emmanuel Renault, CNRS, 2020
* @author Erwan Bocher, CNRS, 2020
*/

/*================================================================================
* DEPENDENCIES
*/
// MAVEN repository
@GrabResolver(name="orbisgis", root="https://nexus.orbisgis.org/repository/orbisgis/")

// GEOCLIMATE dependencies
@Grab(group="org.orbisgis.orbisprocess", module="geoclimate", version="1.0.0-SNAPSHOT")

// JSON dependencies
@Grab(group="org.codehaus.groovy", module="groovy-json", version="3.0.4")

// JDBC dependencies
@Grab(group="org.orbisgis.orbisdata.datamanager", module="jdbc", version="1.0.1-SNAPSHOT")

import org.orbisgis.orbisprocess.geoclimate.Geoclimate
//import org.orbisgis.orbisdata.datamanager.jdbc.postgis.POSTGIS
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS
import groovy.json.JsonOutput

/*================================================================================
* OUTPUT PATHS
*/
String outputDirectory = "/tmp/osm/"
File outdirFile = new File(outputDirectory)
outdirFile.delete()
outdirFile.mkdir()

/*================================================================================
* OSM filter supports as input place name or bbox
* e.g def osmFilters = [[47.654114,-2.764907,47.661746,-2.750273]]
* e.g def osmFilters = ["vannes", "redon"]
*/

//def osmFilters = [[48.813420,2.220440,48.904449,2.471581]]
def osmFilters = ["vannes"]
/*================================================================================
* output folder and files
*/
def  output = [
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

/*================================================================================
* PARAMETERS
*/
def osm_parameters = [
        "description" :"Run the OSM workflow and store the results in ${outputDirectory}",
        "geoclimatedb" : [
                "folder" :"${outdirFile.absolutePath}",
                "name" : "geoclimate_db;AUTO_SERVER=TRUE",
                "delete" : true
        ],
        "input" : [
                "osm" : osmFilters
        ],
        "output" : output,
        "parameters": [
                "distance": 0,
                "indicatorUse": ["LCZ"],
                "svfSimplified": false,
                "prefixName": "",
                "hLevMin": 3,
                "hLevMax": 15,
                "hThresholdLev2": 10
        ]
]

/*================================================================================
* GEOCLIMATE processing chain
*/
def process = Geoclimate.OSM.workflow
def isValidProcess = process.execute(configurationFile:createOSMConfigFile(osm_parameters, outputDirectory))

if (isValidProcess) {
    //Re-open the local H2GIS database
    def h2GIS = H2GIS.open("${outdirFile.absolutePath+File.separator+"geoclimate_db;AUTO_SERVER=TRUE"}")
   
    //Iterate over each osm filters to compute the indicators
    if (osmFilters && osmFilters in Collection) {
        osmFilters.eachWithIndex { osmFilter, index ->
        
            //SubFolder name
            def folderName = osmFilter in Map?osmFilter.join("_"):osmFilter
            def subFolder = new File(outdirFile.absolutePath+File.separator+"osm_"+folderName)
            
            if (subFolder.exists()) {
                //Process all result folders
                //Load saved geojson files. Must be changed in the future to get the tables that are already in the database
                h2GIS.load("${subFolder.absolutePath+File.separator+"zones.geojson"}",     "zones", true)
                h2GIS.load("${subFolder.absolutePath+File.separator+"rsu_lcz.geojson"}", "rsu_lcz", true)

                // Make gridded domain with 1000x1000 m2 cells
                // Note that the grid must be computed in the SRID UTM zone of the processed domain, not in WGS84
                def gridPrefix = "copernicus"
                def gridProcess = Geoclimate.SpatialUnits.createGrid()

                //Get the extend of the zone table. We think that the have only one area in the zone table
                //Must be updated in the future
                def box =  h2GIS.getSpatialTable("zones").getExtent()
                if (gridProcess.execute(
                        [geometry: box,
                         deltaX    : 1000,
                         deltaY    : 1000,
                         prefixName: gridPrefix,
                         datasource: h2GIS])) {

                    // Make aggregation process with previous grid and current rsu area
                    def targetTableName    = gridProcess.results.outputTableName
                    def indicatorTableName = "rsu_lcz"
                    def indicatorName      = "lcz1"

                    def upperScaleAreaStatistics = Geoclimate.GenericIndicators.upperScaleAreaStatistics()
                    if (upperScaleAreaStatistics.execute(
                            [upperTableName: targetTableName,
                             upperColumnId : "id",
                             lowerTableName: indicatorTableName,
                             lowerColumName: indicatorName,
                             prefixName    : "agg",
                             datasource    : h2GIS])){
                        h2GIS.getSpatialTable(upperScaleAreaStatistics.results.outputTableName).save(
                            "${subFolder.absolutePath+File.separator+"grid_rsu_lcz.geojson"}", true)

                    } else {
                        println("Cannot aggregate the LCZ data at the grid scale ${box}")
                    }
                } else {
                    println("Cannot compute the grid on the area ${box}")
                }
            }
        }
    }
    h2GIS.close()
} else {
    println('Geoclimate process invalid')
}

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
    if (configFile.exists()){
        configFile.delete()
    }
    configFile.write(JsonOutput.prettyPrint(json))
    return configFile.absolutePath
}
