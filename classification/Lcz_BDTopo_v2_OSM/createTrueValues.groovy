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
def pathCitiesToTreat = configFileWorkflowPath + File.separator + this.args[1]
def outputFolderTrueValueConfigFile = this.args[2]
def dataTrueValues = this.args[3].toUpperCase()
def dbUrlTrueValues = this.args[4]
def dbIdTrueValues = this.args[5]
def dbPasswordTrueValues = this.args[6]
def pathToSaveTrueValues = this.args[7]
Integer resetDatasetTrueValue = this.args[8]
def indicatorUseTrueValue = this.args[9].replaceAll(" ", "").split(",")

// Define some table names
def dependentVarTableName = "dependentVarTableName"
def dependentVarTableNameRaw = "dependentVarTableName_raw"

////////////////////////////////////////////////////////////////////
// Start to operate processes //////////////////////////////////////
////////////////////////////////////////////////////////////////////
// Prepare and launch the workflow
ggf.executeWorkflow(configFilePathProduceTrueValues, pathCitiesToTreat, outputFolderTrueValueConfigFile, dataTrueValues, indicatorUseTrueValue, dbUrlTrueValues, dbIdTrueValues, dbPasswordTrueValues, resetDatasetTrueValue)

// Open a database used for calculation and load the IAUIdF file (LCZ)
H2GIS datasource = H2GIS.open("/tmp/trueValue${ggf.getUuid()};AUTO_SERVER=TRUE", "sa", "")
datasource.load(dependentVariablePath, dependentVarTableNameRaw)

