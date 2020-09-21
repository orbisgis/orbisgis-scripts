#!/bin/bash

#############################################################
# Path of the data location
#############################################################
# File where are stored the list of cities to process (note that the cities to be processed should be String - even for insee codes - separated by comma and the file should be located in the same folder as the current file)
nameFileCities="test_cities.csv"
outputFolder="/home/decide/Data/URBIO/Donnees_brutes/LCZ/TrainingDataSets/Indicators/"
dependentVariablePath="/home/decide/Documents/CloudS/LABSTICC/ClassificationSupervisee/Data/data_apprentissage.shp"
pathToSaveTrainingDataSet="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/OSM/MaPuce/TrainingDataset/"

##############################################################"
# Parameters to set
##############################################################
# II. TO CREATE THE WHOLE TRAINING DATASET
# Scale of the dataset used to train the model (possible values: "BUILDING" or "RSU")
scaleTrainingDataset="BUILDING"
# If 'scaleTrainingDataset="RSU"', operations to apply to go gather indicators to a unique scale (see the documentation concerning the IProcess unweightedOperationFromLowerScale() to know which parameters are accepted)
operationsToApply="AVG, STD"
# If 'resetDataset'=0, do not re-calculate the indicators for cities having already results stored in the 'pathToSaveTrainingDataSet' folder
resetDataset=0
# Parameters of the workflow configuration file (only URBAN TYPOLOGY for most cases, LCZ if the "not statistical LCZ algorithm" should also be applied)
indicatorUse="URBAN_TYPOLOGY"
# Dataset to use ("OSM" or "BDTOPO_V2")
data="OSM"
# Name of the dependent variable in the table
dependentVariableColName="I_TYPO"
# Name of geometric field in the table where is stored the dependent variables
geometryField="THE_GEOM"
# Srid of the dependent variable dataset
sridDependentVarIndic="2154"
# Table of correspondence between the values from the dependent variable table and values that should be used in the future (put the same value if you want them to be the same). Note that all values will be necessarily converted to string since they will be used as column values in the code
correspondenceTable="ba: 1,bgh: 2,icif: 3,icio: 4,id: 5,local: 6,pcif: 7,pcio: 8,pd: 9,psc: 10"
# The Dependent variable may have several possible values by order of priority. If you want to focus the training only for smaples having one value, you may give the name of the variable storing the second possible value and the SQL value where it has no value... Note that if there is no second value to use, 'dependentVariable2ndColNameAndVal' should be equal to "default" 
dependentVariable2ndColNameAndVal="default"
# If the randomForest is a classification (classif="true") or a regression (classif="false")
classif="true"

# III. SENSITIVITY ANALYSIS OF THE RANDOM FOREST
pathToSaveResultSensit="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/BDTOPO_V2/MaPuce/ResultsSensitivityAnalysis/"

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

groovy "./calculateIndependentVariables.groovy" "$currentFolder" "$nameFileCities" "$outputFolder" "$data" "$indicatorUse" "$dbUrl" "$dbId" "$dbPassword" "$operationsToApply" "$dependentVariablePath" "$dependentVariableColName" "$geometryField" "$sridDependentVarIndic" "$pathToSaveTrainingDataSet" "$correspondenceTable" "$dependentVariable2ndColNameAndVal" "$resetDataset" "$scaleTrainingDataset" "$classif" "$optionalinputFilePrefix"

echo -e "\n\n\nThe calculation of the independent variables has been performed"


# III. OPTIMIZING THE RANDOM FOREST WITH PYTHON
# Sensitivity analysis on random forest parameters to identify what is the optimum RF parameters for this problem
echo -e "Python script is executing (data analysis to identify the best configuration for the RandomForest model)...\n\n\n"
python "./classification_investigation.py" "$scaleTrainingDataset" "$dependentVariableColName" "$pathToSaveTrainingDataSet" "$data" "$currentFolder" "$pathToSaveResultSensit" "$classif"

echo -e "Results from the sensitivity analysis (to identify the best configuration for the RandomForest) have been saved...\n\n\n"
