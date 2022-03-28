// Declaration of the maven repository
@GrabResolver(name='orbisgis', root='https://nexus.orbisgis.org/repository/orbisgis/')

// Declaration of our Nexus repository, where the geoclimate project is stored
@Grab(group='org.orbisgis.geoclimate', module='geoclimate', version='1.0.0-SNAPSHOT')

//JSON lib
@Grab(group='org.codehaus.groovy', module='groovy-json', version='3.0.4')

import org.orbisgis.geoclimate.Geoclimate
import org.orbisgis.geoclimate.Geoindicators
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
def inputDirectory = this.args[0]
def optionalinputFileSuffix = this.args[1]
def outputFilePathAndName = this.args[2]
def dataset = this.args[3].toUpperCase()
def thresholdColumn = [:]
thresholdColumn[this.args[4].split(":")[0]] = this.args[4].split(":")[1]
def var2Model = this.args[5]
def fileNameColumnsToKeep = this.args[6]
def correspondenceTable = this.args[7]
def pathFileCitiesIndep = this.args[8]
Integer datasetByCityContainsDataset = this.args[9].toInteger()
Map correspondenceValMap = [:]
if(correspondenceTable.contains(":")){
	correspondenceTable = correspondenceTable.replaceAll(" ", "").split(",")
	// Convert the list into a Map to have the key and corresponding value
	correspondenceTable.each{
		def keyAndVal = it.split(":")
		correspondenceValMap[keyAndVal[0]] = keyAndVal[1]
	}
}

////////////////////////////////////////////////////////////////////
// Start to operate processes //////////////////////////////////////
////////////////////////////////////////////////////////////////////
// Recover the columns to keep in the dataset
def line
def lineWithSpaces
new File(fileNameColumnsToKeep).withReader('UTF-8') { reader ->
	line = reader.readLine().replace('"', '').replace("é", "e").replace("'", "_").replace(" ", "_")
}
def columnsToKeep = line.split(",")

// Make the union of the datasets
ggf.unionCities(inputDirectory, optionalinputFileSuffix, outputFilePathAndName, dataset, thresholdColumn, var2Model, columnsToKeep, correspondenceValMap, pathFileCitiesIndep, datasetByCityContainsDataset)
