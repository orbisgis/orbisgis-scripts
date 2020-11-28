/*
* This script is used to process the Geoclimate chain as part of the SLIM project (C3S consortium).
* It has been implemented to make statistics on urban geoindicators for several UTM zones 
* at European scale, to provide information at high spatial resolution (downscaling).
*
* Decription of the process
* ==============================
* We previously generated a regular continuous mesh that is made of domains
* of 1/12° spatial resolution (~10km).
* A Land-Sea Mask provided by ECMWF has been applied to the mesh, which allow to keep only domains
* with 'in-land' grid points (Domains having only 'in-water' grid points are not taken into account).
*
* Specification of the algorithm
* ------------------------------  
* 1. Assigning a list of bounding boxes coordinates:
*      - corresponding to 10x10 km² domains.
*      - storing the list to a variable defined in this configuration file.
* 2. Execution of the script to:
*      a. run the Geoclimate chain for OSM workflow.
*      b. to extract OSM data of the selected domains.
*      c. to compute all the Geoindicators at RSU scale.
* 3. Execution of a process to:
*      - cover each domain with the creation of a grid of 1x1 km² cells.
* 4. Execution of a process to:
*      - fill each cell of the domain by applying spatial aggregation to a given indicator: 
*        LCZs, building height, building density, etc.
*     
* Inputs
* ------------------------------
* This script can parse an input file at Json format, which contains
* the list of the bounding boxes coordinates.
*
* Outputs
* ------------------------------
* Save of the results :
*     - at Geojson format
*     - one file per domain in a sub-directory : /tmp/osm/osm_[bbox_coordinates]
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
@Grab(group="org.orbisgis.orbisprocess", module="geoclimate", version="1.0.0-SNAPSHOT", 
      classifier="jar-with-dependencies", transitive=false)

// JSON dependencies
@Grab(group="org.codehaus.groovy", module="groovy-json", version="3.0.4")

// JDBC dependencies
@Grab(group="org.orbisgis.orbisdata.datamanager", module="jdbc", version="1.0.1-SNAPSHOT")

import org.orbisgis.orbisprocess.geoclimate.Geoclimate
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/*================================================================================
* OUTPUT PATHS
*/
String wrkdir = "/home/ms/copext/cyem/"

String directory = "/perm/ms/copext/cyem/geoclimate_chain/"
File dirFile = new File(directory)
dirFile.delete()
dirFile.mkdir()

String outputDirectory = "/perm/ms/copext/cyem/osm/"
File outdirFile = new File(outputDirectory)
outdirFile.delete()
outdirFile.mkdir()

/*================================================================================
* OSM FILTERS supports as input place name or bbox
* e.g def osmFilters = [[48.83333,2.33333,48.91667,2.41667]]
*/
def osmFiltersList = createOSMFiltersList("osmFilters.json", wrkdir)
def osmFilters = osmFiltersList

osmFilters.each {
    new File(outdirFile, "_$it").deleteDir()
}
new File(outdirFile, "geoclimate_db.mv.db").delete()

/*================================================================================
* OUTPUT FOLDER AND FILES
*/
String location = "folder" //"folder" or "database"
def output = null

switch(location) {
    case "database":
        output = [
            "database": [ 
                "user": "postgres",
                "password": "postgres",
                "url": "jdbc:postgresql://localhost:5432/", 
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
        "description" :"run the OSM workflow and store the results in ${outputDirectory}",
        "geoclimatedb" : [
                "folder" :"${dirFile.absolutePath}",
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
                h2GIS.load("${subFolder.absolutePath+File.separator+"zones.geojson"}"  , "zones"  , true)
                h2GIS.load("${subFolder.absolutePath+File.separator+"rsu_lcz.geojson"}", "rsu_lcz", true)

                // Make gridded domain with 1000x1000 m2 cells
                // Note that the grid must be computed in the SRID UTM zone of the processed domain, not in WGS84
                def gridPrefix = "copernicus_${index}"
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
                  
                    //Assign Zone SRID to Grid SRID
                    def zoneSRID = h2GIS.getSpatialTable("zones").getExtent().getSRID()
                    def targetTableName    = gridProcess.results.outputTableName
                    h2GIS.getSpatialTable(targetTableName).setSrid(zoneSRID)

                    //Make aggregation process on the gridded domain at rsu scale
                    def indicatorTableName = "rsu_lcz"
                    def indicatorName      = "lcz1"
                    def statProcess = Geoclimate.GenericIndicators.upperScaleAreaStatistics()
                    if (statProcess.execute(
                            [upperTableName: targetTableName,
                             upperColumnId : "id",
                             lowerTableName: indicatorTableName,
                             lowerColumName: indicatorName,
                             prefixName    : gridPrefix,
                             datasource    : h2GIS])) {
                        h2GIS.getSpatialTable(statProcess.results.outputTableName).save(
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
} else {
    println("Geoclimate process invalid")
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
    if(configFile.exists()){
        configFile.delete()
    }
    configFile.write(JsonOutput.prettyPrint(json))
    return configFile.absolutePath
}

/**
* Create a list of OSM filters from json file
* @param osmFiltersFile
* @param directory
* @return 
*/
def createOSMFiltersList(def osmFiltersFile, def directory) {
    def jsonFile =  new File(directory+osmFiltersFile)
    if (jsonFile.exists() && jsonFile.length()>0) {
        def jsonSlurper = new jsonSlurper()
        def data = jsonSlurper.parse(jsonFile)
        def osmFilters = jsonSlurper.parseText(JsonOutput.toJson(data))
        def osmFiltersList = []
        N = osmFilters["N"]
        N.times {
            osmFiltersList.add(osmFilters["${it}"]["bbox"])    
        }
        return osmFiltersList
    } else {
    println("File ${jsonFile} does not exist or is empty")
    }
}

