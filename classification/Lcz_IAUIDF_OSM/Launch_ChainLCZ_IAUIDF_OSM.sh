#!/bin/bash

#############################################################
# Path of the data location
#############################################################
# File where are stored the list of cities to process (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCities="allCities.csv"
outputFolder="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/"
dependentVariablePath="/home/decide/Data/URBIO/Donnees_brutes/LCZ/IAUIdF/LivraisonMeteoFrance/donnВes/LCZ.shp"
pathToSaveTrainingDataSet="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/TrainingDataset/"

##############################################################"
# Parameters to set
##############################################################
# Scale of the dataset used to train the model (possible values: "BUILDING" or "RSU")
scaleTrainingDataset="RSU"
# If 'scaleTrainingDataset="RSU"', operations to apply to go gather indicators to a unique scale (see the documentation concerning the IProcess unweightedOperationFromLowerScale() to know which parameters are accepted)
operationsToApply="AVG, STD"
# If 'resetDataset'=0, do not re-calculate the indicators for cities having already results stored in the 'pathToSaveTrainingDataSet' folder
resetDataset=0
# Parameters of the workflow configuration file (only URBAN TYPOLOGY for most cases, LCZ if the "not statistical LCZ algorithm" should also be applied)
indicatorUse="URBAN_TYPOLOGY"
# Dataset to use ("OSM" or "BDTOPO_V2")
data="OSM"
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

if [ -z "$directoryClassif" ];
then
	directoryClassif=$PWD
	cd $directoryClassif
	cd ../
else
	cd $directoryClassif
fi

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

groovy "./calculateIndependentVariables.groovy" $currentFolder $nameFileCities $outputFolder $data $indicatorUse $dbUrl $dbId "$dbPassword" "$operationsToApply" $dependentVariablePath $dependentVariableColName $geometryField $sridDependentVarIndic $pathToSaveTrainingDataSet "$correspondenceTable" "$dependentVariable2ndColNameAndVal" $resetDataset $scaleTrainingDataset

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
