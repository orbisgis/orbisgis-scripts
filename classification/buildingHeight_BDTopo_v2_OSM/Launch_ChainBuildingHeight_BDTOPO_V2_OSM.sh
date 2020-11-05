#!/bin/bash

#############################################################
# Path of the data location
#############################################################
# I. Path to save the dependent variable for each city (created from the BDTOPO_V2 data) and the final true value dataset
outputFolderTrueValueConfigFile="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/Indicators/"
pathToSaveTrueValues="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/TrueValues/"
# File where are stored the list of cities to process for the independent variable dataset (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCitiesDep="allCitiesBDTOPO_V2.csv"

# II. Path for the independent variables and the training dataset
independentVarOutputFolder="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/Indicators/"
pathToSaveTrainingDataSet="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/DatasetByCity/"
# File where are stored the list of cities to process for the independent variable dataset (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCitiesIndep="allCitiesOSM.csv"



##############################################################"
# Parameters to set
##############################################################
# I. TO CREATE THE DEPENDENT VARIABLE DATASET
dataTrueValues="BDTOPO_V2"
# If 'resetDatasetTrueValue'=0, do not re-calculate the dependent variable for cities having already results stored in the 'outputFolderTrueValueConfigFile' folder
resetDatasetTrueValue=0
indicatorUseTrueValue="HEIGHT_ROOF"
# If you want to modify the initial TrueValue to an other system, fill in the correspondence table (if not leave "")
correspondenceTableTrueValue=""
# String to add at the end of the inputDirectory to get the right file (for example "/rsu_lcz.geojson" for the LCZ of BDTOPO_V2)
optionalinputFilePrefixTrueVal="/building.geojson"
# Where to save the dataset that will be used as true values for the training (without the extension)
outputFilePathAndName="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/TrueValues/BUILDING_HEIGHT_OSM_dataset20201020"
# Map containing as key a field name and as value a threshold value below which data will be removed
thresholdColumnTrueValue=""
# The name of the variable to model
var2ModelTrueValue="HEIGHT_ROOF"
# List of columns to keep (except the 'varToModel' which is automatically added)
columnsToKeep="THE_GEOM"
# Whether of not the filename containing the dataset by city contains the datasetName ('BDTOPO_V2' or 'OSM')
datasetByCityContainsDatasetDep=1

# II. TO CREATE THE WHOLE TRAINING DATASET
# Scale of the dataset used to train the model (possible values: "BUILDING" or "RSU")
scaleTrainingDataset="BUILDING"
# If the randomForest is a classification (classif="true") or a regression (classif="false")
classif="false"
# If 'scaleTrainingDataset="RSU"', operations to apply to go gather indicators to a unique scale (see the documentation concerning the IProcess unweightedOperationFromLowerScale() to know which parameters are accepted)
operationsToApply="AVG, STD"
# If 'resetDataset'=0, do not re-calculate the indicators for cities having already results stored in the 'pathToSaveTrainingDataSet' folder
resetDataset=0
# Parameters of the workflow configuration file (only URBAN TYPOLOGY for most cases, LCZ if the "not statistical LCZ algorithm" should also be applied)
indicatorUse="URBAN_TYPOLOGY"
# Dataset to use ("OSM" or "BDTOPO_V2")
data="OSM"
# Name of the dependent variable in the table
dependentVariableColName="HEIGHT_ROOF"
# Name of geometric field in the table where is stored the dependent variables
geometryField="THE_GEOM"
# Srid of the dependent variable dataset
sridDependentVarIndic="2154"
# Table of correspondence between the values from the dependent variable table and values that should be used in the future (put the same value if you want them to be the same). Note that all values will be necessarily converted to string since they will be used as column values in the code ("VAL" + value)
correspondenceTable=""
# The Dependent variable may have several possible values by order of priority. If you want to focus the training only for smaples having one value, you may give the name of the variable storing the second possible value and the SQL value where it has no value... Note that if there is no second value to use, 'dependentVariable2ndColNameAndVal' should be equal to "default" 
dependentVariable2ndColNameAndVal="default"
# String to add at the end of the inputDirectory to get the right file (for example "/osm_building.geojson" for the LCZ of OSM)
optionalinputFilePrefix="/building.geojson"

# III. SENSITIVITY ANALYSIS OF THE RANDOM FOREST
resetSensitivityAnalysis=0
pathToSaveResultSensit="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/ResultsSensitivityAnalysis/"

# IV. CREATE THE FINAL DATASET WITH ALL CITIES
# Whether of not the filename containing the dataset by city contains the datasetName ('BDTOPO_V2' or 'OSM')
datasetByCityContainsDataset=0
# File path to save the resulting dataset WITHOUT THE EXTENSION !!
pathToSaveFinalDataset="/home/decide/Code/Intel/geoclimate/models/TRAINING_DATA_BUILDINGHEIGHT_OSM_RF_1_0"
thresholdCol="UNIQUENESS_VALUE:0.95"
# File where are saved all columns to use as independent variables for the training
fileNameCol2keep="cols2Keep.csv"
optionalinputFileSuffix=".geojson"

##############################################################"
# Start the scripts
##############################################################
# Get the folder where is stored this script
read -p"Please enter the absolute path of the directory containing this script  (if you press enter, get current directory)" currentFolder
echo -e "You selected '$currentFolder'\n\n"
currentFolder=${currentFolder:-$PWD}

read -p"Please enter the absolute path of the 'classification' directory (if you press enter, get the parent directory of the current directory)" directoryClassif
echo -e "You selected '$directoryClassif'"

if [ -z "$directoryClassif" ];
then
	directoryClassif=$PWD
	cd $directoryClassif
	cd ../
else
	cd $directoryClassif
fi


# I. CREATE THE DEPENDENT VARIABLE DATASET
# If the data used for the dependent variable should be recovered on a specific database, set database parameters
read -p"Please enter the absolute url of the database where to find the data used to create THE DEPENDENT VARIABLE DATASET (Press enter if not concerned)" dbUrlTrueValues
if [ -z "$dbUrlTrueValues" ]
then
	dbUrlTrueValues="default"
	dbIdTrueValues="default"
	dbPasswordTrueValues="default"
else
	read -p"Please enter the ID to access the database" dbIdTrueValues
	if [ -z "$dbIdTrueValues" ]
	then
		dbIdTrueValues="default"
	fi
	read -p"Please enter the password corresponding to the ID you gave" dbPasswordTrueValues
	if [ -z "$dbPasswordTrueValues" ]
	then
		dbPasswordTrueValues="default"
	fi
fi

# Execute the script to calculate the independent variables
echo -e "Groovy script is executing (calculation of the dependent variable)...\n\n\n"


groovy "./createTrueValues.groovy" "$currentFolder" "$nameFileCitiesDep" "$outputFolderTrueValueConfigFile" "$dataTrueValues" "$dbUrlTrueValues" "$dbIdTrueValues" "$dbPasswordTrueValues" "$pathToSaveTrueValues" "$resetDatasetTrueValue" "$indicatorUseTrueValue" "$correspondenceTableTrueValue"  "$optionalinputFilePrefixTrueVal" "$outputFilePathAndName" "$thresholdColumnTrueValue" "$var2ModelTrueValue" "$columnsToKeep" "$currentFolder/$nameFileCitiesDep" "$datasetByCityContainsDatasetDep"

echo -e "\n\n\nThe calculation of the dependent variable has been performed"


# II. CREATE THE WHOLE TRAINING DATASET
# If the data should be recovered on a specific database, set database parameters
read -p"Please enter the absolute url of the database where to find the data (Press enter if not concerned)" dbUrl
if [ -z "$dbUrl" ];
then
	dbUrl="default"
	dbId="default"
	dbPassword="default"
else
	read -p"Please enter the ID to access the database" dbId
	if [ -z "$dbId" ];
	then
		dbId="default"
	fi
	read -p"Please enter the password corresponding to the ID you gave" dbPassword
	if [ -z "$dbPassword" ];
	then
		dbPassword="default"
	fi
fi

# Execute the script to calculate the independent variables
echo -e "Groovy script is executing (calculation of the independent variables)...\n\n\n"

groovy "./calculateIndependentVariables.groovy" "$currentFolder" "$nameFileCitiesIndep" "$independentVarOutputFolder" "$data" "$indicatorUse" "$dbUrl" "$dbId" "$dbPassword" "$operationsToApply" "$outputFilePathAndName" "$dependentVariableColName" "$geometryField" "$sridDependentVarIndic" "$pathToSaveTrainingDataSet" "$correspondenceTable" "$dependentVariable2ndColNameAndVal" "$resetDataset" "$scaleTrainingDataset" "$classif" "$optionalinputFilePrefix"

echo -e "\n\n\nThe calculation of the independent variables has been performed"


# III. OPTIMIZING THE RANDOM FOREST WITH PYTHON
# Sensitivity analysis on random forest parameters to identify what is the optimum RF parameters for this problem
if [ $resetSensitivityAnalysis -eq 1 ];
then
	echo -e "Python script is executing (data analysis to identify the best configuration for the RandomForest model)...\n\n\n"
	python "./classification_investigation.py" "$scaleTrainingDataset" "$dependentVariableColName" "$pathToSaveTrainingDataSet" "$data" "$currentFolder" "$pathToSaveResultSensit" "$classif"
	echo -e "Results from the sensitivity analysis (to identify the best configuration for the RandomForest) have been saved...\n\n\n"
else
	echo -e "The sensitivity analysis is not performed since the 'resetSensitivityAnalysis' parameter is not set to 1"
fi


# IV. CREATE THE FINAL DATASET THAT WILL BE USED FOR THE TRAINING
echo -e "Groovy script is executing (union all selected city dataset to create the final dataset that will be used for the training)...\n\n\n"
groovy "./createFinalDataset.groovy" "$pathToSaveTrainingDataSet" "$optionalinputFileSuffix" "$pathToSaveFinalDataset" "$data" "$thresholdCol" "$dependentVariableColName" "$currentFolder/$fileNameCol2keep" "$correspondenceTable" "$currentFolder/$nameFileCitiesIndep" "$datasetByCityContainsDataset"

echo -e "The final dataset has been saved...\n\n\n"
