#!/bin/bash

#############################################################
# Path of the data location
#############################################################
# I. Path to save the dependent variable for each city (created from the BDTOPO_V2 data) and the final true value dataset
outputFolderTrueValueConfigFile="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/"
pathToSaveTrueValues="/home/decide/Data/URBIO/Donnees_brutes/LCZ/BDTOPO_V2/"
# File where are stored the list of cities to process for the independent variable dataset (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCitiesDep="allCitiesBDTOPO_V2.csv"

# II. Path for the independent variables and the training dataset
independentVarOutputFolder="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/"
pathToSaveTrainingDataSet="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/TrainingDataset/"
# File where are stored the list of cities to process for the independent variable dataset (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCitiesIndep="allCitiesOSM.csv"

##############################################################"
# Parameters to set
##############################################################
# I. To create the dependent variable dataset
dataTrueValues="BDTOPO_V2"
# If 'resetDatasetTrueValue'=0, do not re-calculate the dependent variable for cities having already results stored in the 'outputFolderTrueValueConfigFile' folder
resetDatasetTrueValue=0
indicatorUseTrueValue="LCZ"

# II. To create the whole training dataset
# Scale of the dataset used to train the model (possible values: "BUILDING" or "RSU")
scaleTrainingDataset="RSU"
# If 'scaleTrainingDataset="RSU"', operations to apply to go gather indicators to a unique scale (see the documentation concerning the IProcess unweightedOperationFromLowerScale() to know which parameters are accepted)
operationsToApply="AVG, STD"
# If 'resetDataset'=0, do not re-calculate the indicators for cities having already results stored in the 'pathToSaveTrainingDataSet' folder
resetDataset=0
# Parameters of the workflow configuration file (only URBAN TYPOLOGY for most cases, LCZ if the "not statistical LCZ algorithm" should also be applied)
indicatorUse="URBAN_TYPOLOGY"
# Name of the dependent variable in the table
dependentVariableColName="TYPE_LCZ"
# Name of geometric field in the table where is stored the dependent variables
geometryField="THE_GEOM"
# Srid of the dependent variable dataset
sridDependentVarIndic="2154"
# Table of correspondence between the values from the dependent variable table and values that should be used in the future (put the same value if you want them to be the same). Note that all values will be necessarily converted to string since they will be used as column values in the code
correspondenceTable="1: 1,2: 2,3: 3,4: 4,5: 5,6: 6,7: 7,8: 8,9: 9,10: 10,A: 101,B: 102,C: 103,D: 104,E: 105,F: 106,G: 107"
# The Dependent variable may have several possible values by order of priority. If you want to focus the training only for smaples having one value, you may give the name of the variable storing the second possible value and the SQL value where it has no value... Note that if there is no second value to use, 'dependentVariable2ndColNameAndVal' should be equal to "default" 
dependentVariable2ndColNameAndVal="LCZ2=' '"

##############################################################"
# Start the scripts
##############################################################
# Get the folder where is stored this script
read -p"Please enter the absolute path of the directory containing this script  (if you press enter, get current directory)" currentFolder
echo -e "You selected '$currentFolder'\n\n"
currentFolder=${currentFolder:-$PWD}

read -p"Please enter the absolute path of the 'classification' directory (if you press enter, get the parent directory of the current directory)" directoryClassif
echo -e "You selected '$directoryClassif'\n\n"

if [ -z "$directoryClassif"];
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
if [ -z "$dbUrlTrueValues"];
then
	dbUrlTrueValues="default"
	dbIdTrueValues="default"
	dbPasswordTrueValues="default"
else
	read -p"Please enter the ID to access the database" dbIdTrueValues
	if [ -z "$dbIdTrueValues"];
	then
		dbIdTrueValues="default"
	fi
	read -p"Please enter the password corresponding to the ID you gave" dbPasswordTrueValues
	if [ -z "$dbPasswordTrueValues"];
	then
		dbPasswordTrueValues="default"
	fi
fi

# Execute the script to calculate the independent variables
echo -e "Groovy script is executing (calculation of the dependent variable)...\n\n\n"

groovy "./createTrueValues.groovy" $configFilePathProduceTrueValues $nameFileCitiesDep $outputFolderTrueValueConfigFile $dataTrueValues $dbUrlTrueValues $dbIdTrueValues $dbPasswordTrueValues $pathToSaveTrueValues $resetDatasetTrueValue

echo -e "\n\n\nThe calculation of the dependent variable has been performed"


# II. CREATE THE WHOLE TRAINING DATASET
# If the data should be recovered on a specific database, set database parameters
read -p"Please enter the absolute url of the database where to find the data (Press enter if not concerned)" dbUrl
if [ -z "$dbUrl"];
then
	dbUrl="default"
	dbId="default"
	dbPassword="default"
else
	read -p"Please enter the ID to access the database" dbId
	if [ -z "$dbId"];
	then
		dbId="default"
	fi
	read -p"Please enter the password corresponding to the ID you gave" dbPassword
	if [ -z "$dbPassword"];
	then
		dbPassword="default"
	fi
fi

# Execute the script to calculate the independent variables
echo -e "Groovy script is executing (calculation of the independent variables)...\n\n\n"

groovy "./calculateIndependentVariables.groovy" $currentFolder $nameFileCitiesIndep $independentVarOutputFolder "OSM" $indicatorUse $dbUrl $dbId $dbPassword "$operationsToApply" $dependentVariablePath $dependentVariableColName $geometryField $sridDependentVarIndic $pathToSaveTrainingDataSet "$correspondenceTable" "$dependentVariable2ndColNameAndVal" $resetDataset $scaleTrainingDataset

echo -e "\n\n\nThe calculation of the independent variables has been performed"

# Sensitivity analysis on random forest parameters to identify what is the optimum RF parameters for this problem
echo -e "Python script is executing (data analysis to identify the best configuration for the RandomForest model)...\n\n\n"
python $dataFolder"/Scripts/Result_analysis_generic.py"

echo -e "Results from the sensitivity analysis (to identify the best configuration for the RandomForest) have been saved...\n\n\n"

# Gather the results from all cities in one table selecting only independent variables that have been identified in the Python script and the spatial units having a minimum level of uniqueness.
echo -e "Groovy script is executing (gather the results from all cities in a unique table which will be the training dataset)...\n\n\n"





echo -e "Python script has been executed\n\n"

echo -e "Groovy script is executed (calculation of the SVF useful for the discussion section of the article)...\n"
groovy $dataFolder"/Scripts/SVF_calculation_discussion.groovy" $dataFolder

echo -e "Groovy script has been executed\n\n"
