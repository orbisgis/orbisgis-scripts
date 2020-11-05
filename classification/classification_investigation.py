#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
This script may be used to investigate which configuration of a Random Forest or a classification tree
optimizes the performance of a classification.

For now it is not a generic script, only valid for a given problem (LCZ using OSM indicators and IAUIDF true values).

Created on May 26, 2020

@author: Jérémy Bernard
"""
#Python functions
import pandas as pd
import numpy as np
import matplotlib.pylab as plt
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import RandomForestRegressor
from sklearn import tree
from sklearn import metrics
import glob
import graphviz
import pydot
from joblib import dump
import sys
import os

#Local functions
import python_model_optimization as pmo

# ===========================================================
# RECOVER COMMAND LINE ARGUMENTS (PASSED BY THE BATCH)
name_object=sys.argv[1]
if name_object=="RSU":
    index_col = "ID_RSU"
if name_object=="BUILDING":
    index_col = "ID_BUILD"    
typoCol=sys.argv[2]
pathData=sys.argv[3]
dataset=sys.argv[4]
fileNpath_defaultval_transfo = "/".join(sys.argv[5].split("/")[0:-1]) + os.sep + "indicator_default_values.csv"
pathResults=sys.argv[6]
classif=sys.argv[7]

"""
################# BUILDING HEIGHT ############################
name_object = "BUILDING"
if name_object=="RSU":
    index_col = "ID_RSU"

if name_object=="BUILDING":
    index_col = "ID_BUILD"  

typoCol="HEIGHT_ROOF"
pathData="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/DatasetByCity/"
dataset="OSM"
fileNpath_defaultval_transfo="/home/decide/Code/orbisgis-scripts/classification/indicator_default_values.csv"
pathResults="/home/decide/Data/URBIO/Donnees_brutes/BuildingHeight/BDTOPO_V2/ResultsSensitivityAnalysis/"
classif="false"

################# URBAN TYPOLOGY BDTOPO_V2 ############################
name_object = "BUILDING"
if name_object=="RSU":
    index_col = "ID_RSU"

if name_object=="BUILDING":
    index_col = "ID_BUILD"  

typoCol="I_TYPO"
pathData="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/BDTOPO_V2/MaPuce/TrainingDataset/"
dataset="BDTOPO_V2"
fileNpath_defaultval_transfo="/home/decide/Code/orbisgis-scripts/classification/indicator_default_values.csv"
pathResults="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/BDTOPO_V2/MaPuce/ResultsSensitivityAnalysis/"
classif="true"

################# URBAN TYPOLOGY OSM ############################
name_object = "BUILDING"
if name_object=="RSU":
    index_col = "ID_RSU"

if name_object=="BUILDING":
    index_col = "ID_BUILD"  

typoCol="I_TYPO"
pathData="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/OSM/MaPuce/TrainingDataset/"
dataset="OSM"
fileNpath_defaultval_transfo="/home/decide/Code/orbisgis-scripts/classification/indicator_default_values.csv"
pathResults="/home/decide/Data/URBIO/Donnees_brutes/UrbanTypo/OSM/MaPuce/ResultsSensitivityAnalysis/"
classif="true"
"""

# ==========================================================
#PARAMETERS TO SET
# Minimum number of objects in the smallest class (for equaliness)
nb_min_class = 46
# Minimum ratio of building used for verification
nb_min_bu_verif = 0.3
uniqueness_val = u"UNIQUENESS_VALUE"

# Default distance to road and building
default_dist_road = 100
default_dist_build = 100

# Columns that need to be converted from string to categorical data
colToConvert = ["BUILD_TYPE", "BUILD_MAIN_USE"]

# Columns to remove because kind of "duplicated" or not necessary
cols2remove =   pd.Index(["ID_RSU", "ID_BUILD", "BUILD_AVG_HEIGHT_WALL","BUILD_STD_HEIGHT_WALL",
                 "BUILD_AVG_VOLUME", "BUILD_AVG_NUMBER_BUILDING_NEIGHBOR",
                "BUILD_AVG_MINIMUM_BUILDING_SPACING","BUILD_ID_RSU",
                "RSU_MAIN_BUILDING_DIRECTION", "BLOCK_MAIN_BUILDING_DIRECTION",
                "BUILD_ID_ZONE","RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H0_10_D90_120",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H10_20_D90_120",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H20_30_D90_120",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H30_40_D90_120",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H40_50_D90_120",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D0_30",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D120_150",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D150_180",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D30_60",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D60_90",
                "RSU_PROJECTED_FACADE_AREA_DISTRIBUTION_H50_D90_120",
                "BLOCK_AVG_AVG_HEIGHT_ROOF_AREA_WEIGHTED",
                "BLOCK_AVG_STD_HEIGHT_ROOF_AREA_WEIGHTED",
                "BLOCK_STD_AVG_HEIGHT_ROOF_AREA_WEIGHTED","BLOCK_STD_STD_HEIGHT_ROOF_AREA_WEIGHTED"])
# Columns to remove because based on a non accurate building height
cols2removeHeight = pd.Index(["ASPECT_RATIO","AVG_HEIGHT_ROOF_AREA_WEIGHTED",
                              "AVG_VOLUME", 
                              "BUILD_AVG_HEIGHT_ROOF","BUILD_STD_HEIGHT_ROOF",
                                "BUILD_STD_VOLUME","BUILD_AVG_FLOOR_AREA",
                                "BUILD_STD_FLOOR_AREA","BUILD_AVG_CONTIGUITY",
                                "BUILD_STD_CONTIGUITY","BUILD_AVG_RAW_COMPACTNESS",
                                "BUILD_STD_RAW_COMPACTNESS", "RSU_AVG_HEIGHT_ROOF_AREA_WEIGHTED",
                                "RSU_STD_HEIGHT_ROOF_AREA_WEIGHTED", "RSU_FREE_EXTERNAL_FACADE_DENSITY",
                                "RSU_BUILDING_VOLUME_DENSITY", "RSU_AVG_VOLUME", 
                                "RSU_GEOM_AVG_HEIGHT_ROOF", "RSU_BUILDING_FLOOR_AREA_DENSITY",
                                "RSU_ASPECT_RATIO", "BUILD_HEIGHT_WALL", "BUILD_HEIGHT_ROOF",
                                "BUILD_NB_LEV", "BUILD_VOLUME", "BUILD_FLOOR_AREA",
                                "BUILD_CONTIGUITY", "BUILD_RAW_COMPACTNESS","BLOCK_FLOOR_AREA",
                                "BLOCK_VOLUME",
                                "BLOCK_NET_COMPACTNESS", "BLOCK_AVG_HEIGHT_ROOF_AREA_WEIGHTED",
                                "BLOCK_STD_HEIGHT_ROOF_AREA_WEIGHTED",
                                "BLOCK_AVG_FLOOR_AREA", "BLOCK_AVG_NET_COMPACTNESS",
                                "BLOCK_AVG_VOLUME", "BLOCK_FLOOR_AREA",
                                "BLOCK_FLOOR_AREA","BLOCK_NET_COMPACTNESS",
                                "BLOCK_STD_FLOOR_AREA","BLOCK_STD_HEIGHT_ROOF_AREA_WEIGHTED",
                                "BLOCK_STD_NET_COMPACTNESS",
                                "BLOCK_STD_VOLUME","BLOCK_VOLUME","BUILD_AVG_COMMON_WALL_FRACTION",
                                "BUILD_AVG_CONTIGUITY","BUILD_AVG_FLOOR_AREA","BUILD_AVG_HEIGHT_ROOF",
                                "BUILD_AVG_HEIGHT_WALL","BUILD_AVG_RAW_COMPACTNESS","BUILD_AVG_VOLUME",
                                "BUILD_CONTIGUITY","BUILD_FLOOR_AREA","BUILD_HEIGHT_ROOF",
                                "BUILD_HEIGHT_WALL","BUILD_NB_LEV","BUILD_RAW_COMPACTNESS",
                                "BUILD_STD_CONTIGUITY","BUILD_STD_FLOOR_AREA","BUILD_STD_HEIGHT_ROOF",
                                "BUILD_STD_HEIGHT_WALL","BUILD_STD_RAW_COMPACTNESS","BUILD_STD_VOLUME",
                                "BUILD_VOLUME","BUILDING_FLOOR_AREA_DENSITY","BUILDING_VOLUME_DENSITY",
                                "FREE_EXTERNAL_FACADE_DENSITY","GEOM_AVG_HEIGHT_ROOF","RSU_ASPECT_RATIO",
                                "RSU_AVG_HEIGHT_ROOF_AREA_WEIGHTED","RSU_AVG_VOLUME",
                                "RSU_BUILDING_FLOOR_AREA_DENSITY","RSU_BUILDING_VOLUME_DENSITY",
                                "RSU_EFFECTIVE_TERRAIN_ROUGHNESS_CLASS","RSU_EFFECTIVE_TERRAIN_ROUGHNESS_LENGTH",
                                "RSU_FREE_EXTERNAL_FACADE_DENSITY","RSU_GEOM_AVG_HEIGHT_ROOF",
                                "RSU_GROUND_SKY_VIEW_FACTOR","RSU_STD_HEIGHT_ROOF_AREA_WEIGHTED",
                                "STD_HEIGHT_ROOF_AREA_WEIGHTED"])
# Remove the indicators based on the height if the dataset used is OSM
if dataset=="OSM":
    cols2remove=cols2remove.union(cols2removeHeight)
if name_object=="BUILDING":
    cols2remove=cols2remove.union(["BUILD_ID_SOURCE", "BUILD_ZINDEX", "ID_BLOCK", "ID_RSU"])
elif name_object=="RSU":
    cols2remove+=[]
colNotIndic = [typoCol, uniqueness_val]


# ==========================================================
#DATA IS LOADED AND UNUSED COLUMNS REMOVED
#Data used for the classification
df_raw = pd.concat([pd.read_csv(f, header = 0, index_col = False)
                    for f in glob.glob(pathData+"*.csv")], 
                    ignore_index = True)

# Remove uneeded columns and remove RSU having no LCZ defined
# (because they do not intersects any IAUIDF rsu that have only one LCZ value)
df = df_raw.drop(cols2remove.intersection(df_raw.columns), axis = 1).dropna(subset = [typoCol])

# Convert the needed variables into categorical data
for var in colToConvert:
    df[var] = df[var].factorize()[0]

#Name of the functions to apply for data distribution transformation are loaded
transfo2apply = pd.read_csv(fileNpath_defaultval_transfo, index_col = 0, \
                            header = 0)["transfo"].reindex(df.columns).dropna()

# Set default values for indicators having NaN
df_default_val = pd.read_csv(fileNpath_defaultval_transfo, index_col = 0, header = 0)
df_default_val.loc["AVG_MINIMUM_BUILDING_SPACING", "default_val"]=default_dist_build
df_default_val.loc["BUILD_AVG_MINIMUM_BUILDING_SPACING", "default_val"]=default_dist_build
df_default_val.loc["BUILD_AVG_ROAD_DISTANCE", "default_val"]=default_dist_road
df_default_val.loc["BUILD_MINIMUM_BUILDING_SPACING", "default_val"]=default_dist_build
df_default_val.loc["BUILD_ROAD_DISTANCE", "default_val"]=default_dist_road
df_default_val.loc["RSU_AVG_MINIMUM_BUILDING_SPACING", "default_val"]=default_dist_build

if df.drop(colNotIndic, axis = 1).columns.difference(df_default_val.index).empty:
    df.fillna(df_default_val["default_val"], axis = 0, inplace=True)
else:
    print "You should define a default value for the following columns of your input data: {0}".format(df.drop(colNotIndic, axis = 1).columns.difference(df_default_val.index))
    list_df = pd.Series(df.drop(colNotIndic, axis = 1).columns.difference(df_default_val.index))
    list_df.to_csv("/home/decide/Bureau/list_indic.csv")

# Minimum number of classified objects (within the entire dataset)
nb_min_default = df.index.size*0.05

# DEFINED SENSITIVITY ANALYSIS
#Constants
initial_indic = df.drop(colNotIndic, axis = 1).columns
nb_class = len(sorted(set(df[typoCol])))

#Parameters for the default case classification
scenari = {"default" : pd.Series({"perc_bu_calib" : [0.7],\
                        "algo2use" : "randomForest",\
                        "model_stat_params" : [{"ntree" : 500, "min_size_node" : 1,\
                                        "nb_var_tree" : 9, "norm_indic" : False,
                                        "max_depth" : 80, "max_leaf_nodes" : 400}],
                        "nb_test" : 5, "indic2use" : {"default" : initial_indic},\
                        "dist_class_typ" : ["REPRESENTATIVE"],\
                        "nb_min" : nb_min_default,
                        "threshold_uniqueness" : [0.7]})}

#We initialize the default parameters for all the classification cases and then
#we set the variations of each case
#3.1 in the report
nbNameObj = "nb_"+name_object
scenari[nbNameObj] = scenari["default"].copy()
scenari[nbNameObj]["perc_bu_calib"] = [0.05, 0.2, 0.35, 0.5]
#3.2 in the report
scenari["distrib_class"] = scenari["default"].copy()
scenari["distrib_class"]["dist_class_typ"] = ["RANDOM"]
#scenari["distrib_class"]["dist_class_typ"] = ["EQUALLY", "REPRESENTATIVE"]
scenari["distrib_class"]["nb_min"] = nb_min_class * nb_class
#3.3 in the report
scenari["uniqueness"] = scenari["default"].copy()
scenari["uniqueness"]["threshold_uniqueness"] = [0.3, 0.5, 0.7, 1.0]
#3.5 in the report
scenari["param_RF"] = scenari["default"].copy()
scenari["param_RF"]["model_stat_params"] = [{"ntree" : nt, "min_size_node" : msn,\
       "nb_var_tree" : nvt, "norm_indic" : ni, "max_depth": md,
       "max_leaf_nodes": mln} for nt in [100, 500, 1000]\
for msn in [1, 3, 5, 7] for nvt in [3,7,9,15] for ni in [False]
for md in [20, 40, 60, 80] for mln in [100, 200, 300, 400]]
scenari["param_RF"]["model_stat_params"].remove(scenari["default"]["model_stat_params"][0])
#3.6 in the report
#scenari["classif_algo"] = scenari["default"].copy()
#scenari["classif_algo"]["algo2use"] = "regressionTree"
#scenari["classif_algo"]["model_stat_params"] = [{"ntree" : None, "min_size_node" : msn,\
#       "nb_var_tree" : nvt, "norm_indic" : ni, "max_depth": md, "max_leaf_nodes": mln} for msn in [1, 2, 3]\
# for nvt in [3,7,10] for ni in [True, False] for md in [20, 40, 60, 80] for mln in [50, 100, 200, 300]]
#scenari["classif_algo"]["model_stat_params"] = [{"ntree" : None, "min_size_node" : msn,\
#       "nb_var_tree" : nvt, "max_depth": md, "max_leaf_nodes": mln} for msn in [1, 2, 3]\
# for nvt in [3,7,10] for md in [20, 40, 60, 80] for mln in [50, 100, 200, 300]]


"""
# Plot the histogram of each indicator by batch of 'nb_indic_per_fig' indicators
nb_indic_per_fig = 12
bin_nb = 20
nb_fig = df.columns.size/nb_indic_per_fig
if df.columns.size%nb_indic_per_fig != 0:
    nb_fig += 1
for i in range(0,nb_fig):
    df2hist = df.loc[:, df.columns[i*nb_indic_per_fig:(i+1)*nb_indic_per_fig]]
    fig, ax = plt.subplots()
    df2hist.hist(bins = bin_nb, ax = ax)

# Test the transformations (transformation to a more "uniform distribution")
df_test_transfo = pmo.AnalyseData.transfo2normal(df.drop(colNotIndic, axis = 1), transfo2apply)
for i in range(0,nb_fig):
    df2hist = df_test_transfo.loc[:, df_test_transfo.columns[i*nb_indic_per_fig:(i+1)*nb_indic_per_fig]]
    fig, ax = plt.subplots()
    df2hist.hist(bins = bin_nb, ax = ax)
"""
print df
# ==========================================================
#CLASSIFICATION TESTS
#Scenari that are calculated
result = pd.DataFrame(columns = ["scenario", "perc_bu_calib", "dist_class_typ",\
                                 "nb_test", "indic2use", "ntree",\
                                 "min_size_node", "nb_var_tree", "max_depth", "max_leaf_nodes", "norm_indic",\
                                 "algo2use", "score_inter", "nb_min", "threshold_uniqueness"])
for s in scenari:
    print "\n\nScenario : " + s
    scenario = scenari[s].copy()
    #For each indicators combination of the scenario
    for ind_case in scenario["indic2use"]:
        #Store the indicators used in the indicator case
        ind = scenario["indic2use"][ind_case]
        df_indic = df[ind.union(colNotIndic)].copy()
        #For each combination of randomForest parameters (if needed)
        for msp in scenario["model_stat_params"]:
            #Temporary variables are created to make the script more digest
            nb_test = scenario["nb_test"]
            algo2use = scenario["algo2use"]
            nb_var_tree = msp["nb_var_tree"]
            ntree = msp["ntree"]
            min_size_node = msp["min_size_node"]
            max_depth = msp["max_depth"]
            max_leaf_nodes = msp["max_leaf_nodes"]
            norm_indic = msp["norm_indic"]
            algo2use = scenario["algo2use"]
            nb_min_intra = scenario["nb_min"]
            
            #Transform the indicator distribution if needed
            if norm_indic is True:
                "Distribution transformation is performed"
                df_transf = pmo.AnalyseData.transfo2normal(df_indic.drop(colNotIndic,\
                                                             axis = 1), transfo2apply).join(df_indic[colNotIndic])
            elif norm_indic is False:
                df_transf = df_indic.copy()
            
            #Create the model object consistent with the classification model to use
            if algo2use == "regressionTree":
                if classif == "false" or classif == "False":
                    clf = tree.DecisionTreeRegressor(max_features = nb_var_tree,\
                                                      min_samples_leaf = min_size_node)
                elif classif == "true" or classif == "True":
                    clf = tree.DecisionTreeClassifier(max_features = nb_var_tree,\
                                                      min_samples_leaf = min_size_node)                    
            elif algo2use == "randomForest":
                if classif == "false" or classif == "False":
                    clf = RandomForestRegressor(n_estimators = ntree, max_features = nb_var_tree,\
                                                 min_samples_leaf = min_size_node, max_depth = max_depth,
                                                 max_leaf_nodes = max_leaf_nodes)
                elif classif == "true" or classif == "True":
                    clf = RandomForestClassifier(n_estimators = ntree, max_features = nb_var_tree,\
                                                 min_samples_leaf = min_size_node, max_depth = max_depth,
                                                 max_leaf_nodes = max_leaf_nodes)   

            
            for thresh_uniqueness in scenario["threshold_uniqueness"]:
                print u"Uniqueness threshold :"+str(thresh_uniqueness*100)+"%" 
                data2use_cal = df_transf.copy()
                for dist_class_typ in scenario["dist_class_typ"]:
                    for perc_bu_calib in scenario["perc_bu_calib"]:
                        print u"Percentage of objects used for calibration :"+str(perc_bu_calib*100)+"%" 
                        #Calibration and verification are performed 'nb_test' times
                        for i in range(0, nb_test):
                            print u"Calibration N° :" + str(i + 1) 
                            # Select the data having the uniqueness value higher than a certain value
                            data2use_i=data2use_cal[data2use_cal[uniqueness_val] >= thresh_uniqueness]
                            #Select data in order to have the same number of items
                            #per city and if final_distrib is "EQUALLY" the same number
                            #of items per class
                            data2use_i = pmo.AnalyseData.select_from_data(data2use_i, nb_data = nb_min_intra, final_distrib =\
                                                                          dist_class_typ, nb_data_class = nb_min_class, 
                                                                          distrib_col_name = typoCol,
                                                                          classif = classif)
                            #Select for calibration a 'perc_bu_calib' building of the previous data2use_i
                            df_calib = pmo.AnalyseData.select_from_data(data2use_i, nb_data = int(data2use_i.index.size*perc_bu_calib),\
                                                                        final_distrib = dist_class_typ, 
                                                                        distrib_col_name = typoCol,
                                                                        classif=classif).drop(uniqueness_val, axis = 1)
                            #Identify the index of the buildings that are not used for calibration
                            index_not_calib = data2use_i.index.difference(df_calib.index)
                            #Select for inter verif a 'perc_bu_calib' building of the previous data2use_i
                            df_verif = pmo.AnalyseData.select_from_data(data2use_i.reindex(index_not_calib), nb_data = \
                                                                        int(data2use_i.index.size*nb_min_bu_verif),\
                                                                        final_distrib = dist_class_typ, 
                                                                        distrib_col_name = typoCol,
                                                                        classif=classif).drop(uniqueness_val, axis = 1)
                            
                            rf_model = clf.fit(df_calib.drop(typoCol, axis = 1), \
                                               df_calib[typoCol])
                            
                            pred_inter = rf_model.predict(df_verif.drop(typoCol, axis = 1))
                            
                            if classif == "false" or classif == "False":
                                score_inter = metrics.mean_absolute_error(df_verif[typoCol], pred_inter)
                            elif classif == "true" or classif == "True":
                                score_inter = metrics.accuracy_score(df_verif[typoCol], pred_inter)
                                
                            
                            result = result.append({"scenario" : s,\
                                                    "perc_bu_calib" : perc_bu_calib,\
                                                    "dist_class_typ" : dist_class_typ,\
                                                    "nb_test" : nb_test,\
                                                    "indic2use" : ind_case,\
                                                    "ntree" : ntree,\
                                                    "min_size_node" : min_size_node,\
                                                    "nb_var_tree" : nb_var_tree,\
                                                    "max_depth" : max_depth,\
                                                    "max_leaf_nodes" : max_leaf_nodes,\
                                                    "norm_indic" : norm_indic,\
                                                    "algo2use" : algo2use,\
                                                    "score_inter" : score_inter,\
                                                    "nb_min" : scenario["nb_min"],
                                                    "threshold_uniqueness" : thresh_uniqueness}, ignore_index = True)
                            del data2use_i
                del data2use_cal
            del df_transf            
        del df_indic
    
result.to_csv(pathResults+"result2.csv")    


# =============================================================
# ========== PRINT THE BEST COMBINATION =======================
# =============================================================
# Identify the 'percentileToKeep' % of best values
percentileToKeep = 0.02
best = result[result.score_inter>result.score_inter.quantile(1-percentileToKeep)][["score_inter", "ntree","min_size_node","nb_var_tree","max_depth","max_leaf_nodes"]]

# Count the number of times each scenario is within the best
nbScenarioInBest = best.groupby(["ntree","min_size_node","nb_var_tree","max_depth","max_leaf_nodes"], as_index=False).size()

# Identify the scenario being the best (having the highest mode)
bestScenario = pd.Series(nbScenarioInBest.idxmax(), index = nbScenarioInBest.index.names)

print bestScenario

# ================================================================
# ========== PRINT ANALYSIS OF THE RESULTS =======================
# ================================================================
verif = "score_inter"

#ANALYSIS OF THE RESULTS
result = pd.read_csv(pathResults+"result2.csv", header = 0, index_col = 0)    

#Scenario 3.1 : influence of the building number used
fin_data = result[(result["scenario"] == nbNameObj)|(result["scenario"]=="default")]
perc_bu_calib = scenari[nbNameObj]["perc_bu_calib"]
perc_bu_calib.append(scenari["default"]["perc_bu_calib"][0])

#We recover the percentage of good prediction (for inter predictions)
#per city and calculate the mean and std for each percentage of object value
indic_fin_city = pd.DataFrame({"mean" : [fin_data[fin_data["perc_bu_calib"]==p]
                                         [verif].mean() for\
                                         p in perc_bu_calib],
                                "std" : [fin_data[fin_data["perc_bu_calib"]==p]
                                         [verif].std() for\
                                         p in perc_bu_calib]}, index = perc_bu_calib)

data2plot = pd.DataFrame({p : fin_data[fin_data["perc_bu_calib"] == p][verif].values\
                                   for p in perc_bu_calib})


#Plot 1 : global view of the number of building effect
fig, ax = plt.subplots(nrows = 1)
color = np.random.rand(3,1).flatten()
bp = ax.boxplot(data2plot.values, positions = data2plot.columns, 
                labels = data2plot.columns, patch_artist=True, boxprops = \
                dict(facecolor = color), flierprops = dict(markeredgecolor = color),\
                whiskerprops = dict(color = color), capprops = dict(color = color))


#Scenario 3.2 : influence of the class distribution
fin_data=result[(result["scenario"]=="distrib_class")|(result["scenario"]=="default")]
dist_class_typ = scenari["distrib_class"]["dist_class_typ"]
dist_class_typ.append(scenari["default"]["dist_class_typ"][0])
#We recover the percentage of good prediction (for inter and extra predictions)
#per type of distrib in order to plot the differences
data2plot = pd.DataFrame({dst : fin_data[fin_data["dist_class_typ"]==dst][verif]\
                                   for dst in dist_class_typ})
     
#Plot 1 : global view of the effect of using equally distributed classes
data2plot.boxplot()


#Scenario 3.3 : influence of the 'uniqueness_threshold' on the results
fin_data=result[(result["scenario"]=="uniqueness")|(result["scenario"]=="default")]
indic_cases = sorted(set(fin_data["threshold_uniqueness"]))
#We recover the percentage of good prediction (for inter and extra predictions)
#per indicator combination case in order to plot the differences
data2plot = pd.DataFrame({ind_case : fin_data[fin_data["threshold_uniqueness"]==ind_case][verif]\
                                   for ind_case in indic_cases})

#Plot 1 : Global results to compare the effect of the 'uniqueness_threshold' used on the classif perf
data2plot.boxplot(positions = data2plot.columns)


#Scenario 3.4 : influence of the indicators used
fin_data=result[(result["scenario"]=="indics")|(result["scenario"]=="default")]
indic_cases = sorted(set(fin_data["indic2use"]))
#We recover the percentage of good prediction (for inter and extra predictions)
#per indicator combination case in order to plot the differences
data2plot = pd.DataFrame({ind_case : fin_data[fin_data["indic2use"]==ind_case][verif]\
                                   for ind_case in indic_cases})

#Plot 1 : Global results to compare the effect of the indicators used on the classif perf
data2plot.boxplot()



#Scenario 3.5 : influence of the randomForest parameters
fin_data=result[(result["scenario"]=="param_RF")|(result["scenario"]=="default")]
model_stat_params = pd.DataFrame(scenari["param_RF"]["model_stat_params"])
#We recover the percentage of good prediction (for inter and extra predictions)
#per per value of each randomForest parameter in order to highligh its influence
data2plot = {p : pd.DataFrame({val_param : \
                                   fin_data[fin_data[p]==val_param][verif].values\
                                   for val_param in sorted(set(fin_data[p]))})
                                            for p in model_stat_params.columns}
    
#Plot 1
for p in model_stat_params.columns:
    fig, ax = plt.subplots()
    fig.suptitle(p)
    if data2plot[p].columns.dtype_str != 'object':
        width = pd.Series(data2plot[p].columns).diff().min()/2
        data2plot[p].boxplot(positions = data2plot[p].columns, widths = width)
    else:
        data2plot[p].boxplot()


#Scenario 3.6 : influence of the classification method (test of regressionTree)
# used and its parameters
fin_data=result[(result["scenario"]=="classif_algo")|(result["scenario"]=="param_RF")|\
                (result["scenario"]=="default")]
fin_data_regtree=fin_data[fin_data["algo2use"]=="regressionTree"]
algo2use = sorted(set(fin_data["algo2use"]))
model_stat_params = pd.DataFrame(scenari["classif_algo"]["model_stat_params"]).dropna(axis = 1)
#We recover the percentage of good prediction (for inter and extra predictions)
#per per value of each regressionTree parameter in order to highligh its influence
data2plot = pd.DataFrame({a2u:fin_data[fin_data["algo2use"]==a2u][verif]\
                                   for a2u in algo2use})

#We recover the percentage of good prediction (for inter and extra predictions)
#per per value of each regressionTree parameter in order to highligh its influence
data2plot_regtree = {p : pd.DataFrame({val_param : \
                           fin_data_regtree[fin_data_regtree[p]==val_param][verif].values\
                           for val_param in sorted(set(fin_data_regtree[p]))})
                                    for p in model_stat_params.columns}

#Plot 1 : Differences between randomForest and regressionTree
data2plot.boxplot()
    
#Plot 2 : Optimisation of regressionTree parameters
for p in model_stat_params.columns:
    fig, ax = plt.subplots()
    fig.suptitle(p)
    if data2plot_regtree[p].columns.dtype_str != 'object':
        width = pd.Series(data2plot_regtree[p].columns).diff().min()/2
        data2plot_regtree[p].boxplot(positions = data2plot_regtree[p].columns, widths = width)
    else:
        data2plot_regtree[p].boxplot()
    
        
# FINAL CHOICE FOR THE BEST CONFIGURATION
perc_bu_calib = 0.7
nb_data2use = 90000
finalParam = {"randomForest": {"indicators2use": scenari["indics"]["indic2use"]["no_height"],
                    "dist_class_type": "REPRESENTATIVE",
                    "uniqueness_threshold": 0.7,
                    "min_size_node": 3,
                    "nb_var_tree": 15,
                    "ntree": 500,
                    "norm_indic": False
                    },
              "regressionTree": {"indicators2use": scenari["indics"]["indic2use"]["no_height"],
                        "dist_class_type": "REPRESENTATIVE",
                        "uniqueness_threshold": 0.7,
                        "min_size_node": 3,
                        "nb_var_tree": 15,
                        "ntree": np.nan,
                        "norm_indic": False
                        }
              }

confusion_matrix = {}
score_inter = {}
rf_model = {}
for algo2use in finalParam.keys():
    print algo2use
    dic_param = finalParam[algo2use]
    df_indic = df[dic_param["indicators2use"].union(colNotIndic)].copy()
    
    thresh_uniqueness = dic_param["uniqueness_threshold"]
    dist_class_typ = dic_param["dist_class_type"]
    nb_var_tree = dic_param["nb_var_tree"]
    ntree = dic_param["ntree"]
    min_size_node = dic_param["min_size_node"]
    norm_indic = dic_param["norm_indic"]
    nb_min_intra = nb_min_default
    
    if dic_param["norm_indic"] is True:
        "Distribution transformation is performed"
        df_transf = pmo.AnalyseData.transfo2normal(df_indic.drop(colNotIndic,\
                                                     axis = 1), transfo2apply).join(df_indic[colNotIndic])
    else:
        df_transf = df_indic.copy()
    
    #Create the model object consistent with the classification model to use
    if algo2use == "regressionTree":
        clf = tree.DecisionTreeClassifier(max_features = nb_var_tree,\
                                          min_samples_leaf = min_size_node)              
    elif algo2use == "randomForest":
        clf = RandomForestClassifier(n_estimators = ntree, max_features = nb_var_tree,\
                                     min_samples_leaf = min_size_node)

    
    data2use_i = pmo.AnalyseData.select_from_data(df_transf, nb_data = nb_data2use,
                                                  final_distrib = dist_class_typ, 
                                                  nb_data_class = np.nan, 
                                                  uniqueness_threshold=thresh_uniqueness,
                                                  uniqueness_col_name=uniqueness_val)
    
    #Select for calibration a 'perc_bu_calib' building of the previous data2use_i
    df_calib = pmo.AnalyseData.select_from_data(data2use_i, nb_data = int(nb_data2use*perc_bu_calib),\
                                                final_distrib = "REPRESENTATIVE", 
                                                uniqueness_threshold=thresh_uniqueness,
                                                uniqueness_col_name=uniqueness_val).drop(uniqueness_val, axis = 1)
    
    #Identify the index of the buildings that are not used for calibration
    index_not_calib = data2use_i.index.difference(df_calib.index)
    #Select for inter verif a 'perc_bu_calib' building of the previous data2use_i
    df_verif = pmo.AnalyseData.select_from_data(data2use_i.reindex(index_not_calib), nb_data = \
                                                int(nb_data2use*nb_min_bu_verif),\
                                                final_distrib = "REPRESENTATIVE", 
                                                uniqueness_threshold=thresh_uniqueness,
                                                uniqueness_col_name=uniqueness_val).drop(uniqueness_val, axis = 1)
                            
    rf_model[algo2use] = clf.fit(df_calib.drop(typoCol, axis = 1), \
                       df_calib[typoCol])
    
    pred_inter = rf_model[algo2use].predict(df_verif.drop(typoCol, axis = 1))
        
    score_inter[algo2use] = metrics.accuracy_score(df_verif[typoCol], pred_inter)
    confusion_matrix[algo2use] = pd.DataFrame(metrics.confusion_matrix(df_verif[typoCol],
                                                                         pred_inter),
                                                columns = sorted(set(df_verif[typoCol])),
                                                index = sorted(set(df_verif[typoCol])))
    
# Save the regression tree into a joblib file
filename = "LczRegressionTree"
dump(rf_model["regressionTree"], pathData+filename+'.joblib') 

# Export the joblist tree as an image (.dot)
dot_data = tree.export_graphviz(rf_model["regressionTree"], out_file=None, 
                                feature_names=dic_param["indicators2use"],  
                                class_names=sorted(set(df_verif[typoCol])),  
                                filled=True, rounded=True,  
                                special_characters=True)

graphviz.Source(dot_data).save(filename = filename+".dot",
                               directory = pathData)
(ImageTree,) = pydot.graph_from_dot_file(pathData+filename+".dot")
ImageTree.write_png(pathData+filename+".png")
