/*###########################################################################################
This script gathers tables from several cities to one selecting only :
	-> some useful columns
	-> spatial units having a certain level of uniqueness value
#############################################################################################*/


@GrabResolver(name='orbisgis', root='https://nexus.orbisgis.org/repository/orbisgis/')
@Grab(group='org.orbisgis.orbisprocess', module='geoclimate', version='1.0.0-SNAPSHOT')

import org.orbisgis.orbisprocess.geoclimate.Geoclimate
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS

// Script to gather ALL communes of IDF in a same dataset (for bdtopo or OSM)
def dataset = "OSM"
def resultsGeoclimate = ["BDTOPO_V2"	: "/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/BDTOPO_V2",
	       		 "OSM"		: "/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/TrainingDataset"]

// Columns to keep for the training (if [], take all columns)
def var2Model = "LCZ"
def thresholdColumn = ["UNIQUENESS_VALUE" : 0.7]
def explicativeVariables = ["BUILD_STD_HEIGHT_WALL","BUILD_AVG_PERIMETER",
       "BUILD_STD_PERIMETER","BUILD_AVG_AREA","BUILD_STD_AREA",
       "BUILD_STD_VOLUME","BUILD_AVG_TOTAL_FACADE_LENGTH",
       "BUILD_STD_TOTAL_FACADE_LENGTH","BUILD_AVG_COMMON_WALL_FRACTION",
       "BUILD_STD_COMMON_WALL_FRACTION",
       "BUILD_STD_NUMBER_BUILDING_NEIGHBOR","BUILD_AVG_AREA_CONCAVITY",
       "BUILD_STD_AREA_CONCAVITY","BUILD_AVG_FORM_FACTOR",
       "BUILD_STD_FORM_FACTOR","BUILD_AVG_PERIMETER_CONVEXITY",
       "BUILD_STD_PERIMETER_CONVEXITY","BUILD_STD_MINIMUM_BUILDING_SPACING",
       "BUILD_AVG_ROAD_DISTANCE","BUILD_STD_ROAD_DISTANCE",
       "BUILD_AVG_LIKELIHOOD_LARGE_BUILDING",
       "BUILD_STD_LIKELIHOOD_LARGE_BUILDING","HIGH_VEGETATION_FRACTION",
       "HIGH_VEGETATION_WATER_FRACTION","HIGH_VEGETATION_BUILDING_FRACTION",
       "HIGH_VEGETATION_LOW_VEGETATION_FRACTION",
       "HIGH_VEGETATION_ROAD_FRACTION",
       "HIGH_VEGETATION_IMPERVIOUS_FRACTION","WATER_FRACTION",
       "BUILDING_FRACTION","LOW_VEGETATION_FRACTION","ROAD_FRACTION",
       "IMPERVIOUS_FRACTION","VEGETATION_FRACTION_URB",
       "LOW_VEGETATION_FRACTION_URB",
       "HIGH_VEGETATION_IMPERVIOUS_FRACTION_URB",
       "HIGH_VEGETATION_PERVIOUS_FRACTION_URB","ROAD_FRACTION_URB",
       "IMPERVIOUS_FRACTION_URB","AREA","GROUND_LINEAR_ROAD_DENSITY",
       "AVG_NUMBER_BUILDING_NEIGHBOR","AVG_MINIMUM_BUILDING_SPACING",
       "BUILDING_NUMBER_DENSITY","BUILDING_TOTAL_FRACTION",
       "BUILDING_DIRECTION_EQUALITY","BUILDING_DIRECTION_UNIQUENESS",
       "BLOCK_AVG_HOLE_AREA_DENSITY","BLOCK_STD_HOLE_AREA_DENSITY",
       "BLOCK_AVG_BUILDING_DIRECTION_EQUALITY",
       "BLOCK_STD_BUILDING_DIRECTION_EQUALITY",
       "BLOCK_AVG_BUILDING_DIRECTION_UNIQUENESS",
       "BLOCK_STD_BUILDING_DIRECTION_UNIQUENESS","BLOCK_AVG_CLOSINGNESS",
       "BLOCK_STD_CLOSINGNESS"]
def nbTableUnion = 100

// START THE SCRIPT
H2GIS datasource = H2GIS.open("/home/decide/Code/Intel/geoclimate/geoindicators/target/model/lczIdf;AUTO_SERVER=TRUE", "sa", "")
def queryPartialGather = ""	
def code

def allVar2Keep = "*"
if (!explicativeVariables.isEmpty()){
	allVar2Keep = "$var2Model,"
	allVar2Keep += explicativeVariables.join(",")
}

File[] processedAreaList = new File(resultsGeoclimate[dataset]).listFiles()
def i = 1
for (areaPath in processedAreaList){	
	if ((i%nbTableUnion)==0){
		ratio = i/nbTableUnion
		datasource.execute """DROP TABLE IF EXISTS ALL_LCZ${ratio.trunc(0)}; CREATE TABLE ALL_LCZ${ratio.trunc(0)} AS ${queryPartialGather[0..-11]}"""
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
		datasource.load("$areaPath/rsu_lcz.geojson", "city$i", true)
	}	

	queryPartialGather += " SELECT $allVar2Keep,${thresholdColumn.keySet()[0]} FROM city$i UNION ALL "
	i+=1
}
ratio = (i/nbTableUnion).trunc(0)+1
datasource.execute """DROP TABLE IF EXISTS ALL_LCZ${ratio}; CREATE TABLE ALL_LCZ${ratio} AS ${queryPartialGather[0..-11]}"""

def tab_nb = []
i = 1
while (i<=ratio){
	tab_nb.add(i)
	i++
}

def queryGather = ""
datasource.execute """DROP TABLE IF EXISTS ALL_LCZ_WITH_THRESHOLD; 
			CREATE TABLE ALL_LCZ_WITH_THRESHOLD 
				AS SELECT $allVar2Keep, ${thresholdColumn.keySet()[0]} 
				FROM ALL_LCZ${tab_nb.join(" UNION ALL SELECT $allVar2Keep, ${thresholdColumn.keySet()[0]} FROM ALL_LCZ")};
			DROP TABLE IF EXISTS ALL_LCZ;
			CREATE INDEX IF NOT EXISTS id_thresh ON ALL_LCZ_WITH_THRESHOLD USING BTREE(${thresholdColumn.keySet()[0]});
			CREATE TABLE ALL_LCZ 
				AS SELECT $allVar2Keep
				FROM ALL_LCZ_WITH_THRESHOLD
				WHERE ${thresholdColumn.keySet()[0]} > ${thresholdColumn.values()[0]};
			ALTER TABLE ALL_LCZ ADD COLUMN the_geom GEOMETRY;"""

// Modify the LCZ values (String to Integer...)
datasource.execute """  CREATE INDEX IF NOT EXISTS id_lcz ON ALL_LCZ USING BTREE($var2Model);
			DROP TABLE IF EXISTS ALL_LCZ_INT;
			CREATE TABLE ALL_LCZ_INT
				AS SELECT 	CAST(CASEWHEN($var2Model='LCZ1',0,
						CASEWHEN($var2Model='LCZ2',1,
						CASEWHEN($var2Model='LCZ3',2,
						CASEWHEN($var2Model='LCZ4',3,
						CASEWHEN($var2Model='LCZ5',4,
						CASEWHEN($var2Model='LCZ6',5,
						CASEWHEN($var2Model='LCZ7',6,
						CASEWHEN($var2Model='LCZ8',7,
						CASEWHEN($var2Model='LCZ9',8,
						CASEWHEN($var2Model='LCZ10',9,
						CASEWHEN($var2Model='LCZA',10,
						CASEWHEN($var2Model='LCZB',11,
						CASEWHEN($var2Model='LCZC',12,
						CASEWHEN($var2Model='LCZD',13,
						CASEWHEN($var2Model='LCZE',14,
						CASEWHEN($var2Model='LCZF',15,
						CASEWHEN($var2Model='LCZG',16,null))))))))))))))))) AS INTEGER) AS $var2Model,
						the_geom,
						${explicativeVariables.join(',')}
				FROM ALL_LCZ;"""

datasource.execute """ CALL GEOJSONWRITE('/home/decide/Code/Intel/geoclimate/models/TRAINING_DATA_LCZ_OSM_RF_1_0.gz', 'ALL_LCZ_INT')"""
