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
def configFilePathProduceTrueValues = this.args[0]
def pathCitiesToTreat = configFilePathProduceTrueValues + File.separator + this.args[1]
def outputFolderTrueValueConfigFile = this.args[2]
def dataTrueValues = this.args[3].toUpperCase()
def dbUrlTrueValues = this.args[4]
def dbIdTrueValues = this.args[5]
def dbPasswordTrueValues = this.args[6]
def pathToSaveTrueValues = this.args[7]
Integer resetDatasetTrueValue = this.args[8].toInteger()
def indicatorUseTrueValue = this.args[9].replaceAll(" ", "").split(",")
def correspondenceValMap = [:]
if(this.args[10]){
	correspondenceTableTrueValue = this.args[10].replaceAll(" ", "").split(",")
	// Convert the list into a Map to have the key and corresponding value
	correspondenceTableTrueValue.each{
		def keyAndVal = it.split(":")
		correspondenceValMap[keyAndVal[0]] = keyAndVal[1]
	}
}
def optionalinputFilePrefix = this.args[11]
def outputFilePathAndName = this.args[12]
def thresholdColumn = [:]
if(this.args[13]){
	thresholdColumn[this.args[13].replaceAll(" ", "").split(":")[0]] = this.args[13].replaceAll(" ", "").split(":")[1]
}
def var2Model = this.args[14]
def columnsToKeep = this.args[15].replaceAll(" ", "").split(",")



// Define some table names
def dependentVarTableName = "dependentVarTableName"
def dependentVarTableNameRaw = "dependentVarTableName_raw"

////////////////////////////////////////////////////////////////////
// Start to operate processes //////////////////////////////////////
////////////////////////////////////////////////////////////////////
File outputSaveTrueValueFile = new File(outputFilePathAndName)
if(!outputSaveTrueValueFile.exists() || resetDatasetTrueValue){
	// Prepare and launch the workflow
	ggf.executeWorkflow(configFilePathProduceTrueValues, pathCitiesToTreat, outputFolderTrueValueConfigFile, dataTrueValues, indicatorUseTrueValue, dbUrlTrueValues, dbIdTrueValues, dbPasswordTrueValues, resetDatasetTrueValue, optionalinputFilePrefix)

	// Gather all cities in a same file to create the TrueValue dataset
	ggf.unionCities(outputFolderTrueValueConfigFile, optionalinputFilePrefix, outputFilePathAndName, dataTrueValues, thresholdColumn, var2Model, columnsToKeep, correspondenceValMap)
}
else{
	println "The file where are saved true value already exists and you set 'resetDatasetTrueValue' to 0, thus no recalculation is performed"
}

