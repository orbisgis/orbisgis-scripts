#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Thu Nov 23 15:41:12 2017

@author: Jérémy Bernard
"""

import pandas as pd
import numpy as np
import random


def transfo2normal(df, transfo2apply):
    """ Transform a distribution in order to make it normal.
    
        Parameters
    _ _ _ _ _ _ _ _ _ _ 
    
            df : pd.DataFrame
                Object containing the data to transform
            transfo2apply : pd.Series
                For each of the df column, a transformation type should be given
                ("LOG", "INV_XY", "LIN" or "INV")
                
        Returns
    _ _ _ _ _ _ _ _ _ _ 
    
            The data transformed"""
    list_fct = sorted(set(transfo2apply))
    
    result = pd.DataFrame(index = df.index)
    
    for f in list_fct:
        transfo = df[transfo2apply[transfo2apply == f].index]
        
        if f == "LOG":
            transfo = np.log(transfo.sub(transfo.min()) + 1)
        elif f == "INV_XY":
            transfo = 1 - transfo
            transfo = (transfo.sub(transfo.min()) + 0.25)**(-1)
        elif f == "INV":
            transfo = (transfo.sub(transfo.min()) + 0.25)**(-1)
        
        result = result.join(transfo)
    
    return result


def select_from_data(df, nb_data = np.nan, nb_data_class = np.nan, distrib_col_name = \
                     "LCZ", final_distrib = "REPRESENTATIVE",
                     classif = "true"):
    """ Select a random sample of 'nb_data' (number of building) respecting the 
    'final_distribution' of 'distrib_col_name' (building typology, LCZ, building height, etc.).
    
        Parameters
    _ _ _ _ _ _ _ _ _ _ 
    
            df : pd.DataFrame
                Object containing the initial data from which the selection should be performed
            nb_data : int, default "NaN"
                Data size to keep (in case 'final_distrib' = "REPRESENTATIVE")
            nb_data_class : int, default "NaN"
                Number of building per class (in case 'final_distrib' = "EQUALLY")
            distrib_col_name : string, default "LCZ"
                Name of the column used for the distribution
            final_distrib : {"REPRESENTATIVE", "EQUALLY", "RANDOM"}, default "REPRESENTATIVE"
                Type of distribution of the distrib_col_name variable between the input and the output data
                    -> "REPRESENTATIVE" : The percentage of data belonging to a specific class ('distrib_col_name' column)
                    should be equal in the input and the output data
                    -> "EQUALLY" : The number of data should be similar between all classes in the output data
                    -> "RANDOM" : The distribution inside each class is not taken into account for the choice
            classif : string, default "false"
                Whether the random forest is a classification or a regression (possible values "False", "false", "True", "true")
        Returns
    _ _ _ _ _ _ _ _ _ _ 
    
            The selected data (pd.DataFrame object)"""
    result = pd.DataFrame(columns = df.columns)
    quantiles_nb = 10

    if final_distrib != "RANDOM":
        if classif == "false" or classif == "False":
            # Keep the quantiles as representative of the distribution
            classes = [df[distrib_col_name].quantile(float(i)/10) for i in range(0,quantiles_nb+1)]
        elif classif == "true" or classif == "True":
            classes = sorted(set(df[distrib_col_name]))
            
    if final_distrib == "REPRESENTATIVE":            
        for i, cl in enumerate(classes):
            if not (i == 0 and classif == "false" or i == 0 and classif == "False"):
                if classif == "false" or classif == "False":
                    buff_class = df[(df[distrib_col_name] >= classes[i-1]) & \
                                      (df[distrib_col_name] <= classes[i])].copy()
                    nb2keep = int(1.0 / quantiles_nb * nb_data)
                elif classif == "true" or classif == "True":
                    buff_class = df[df[distrib_col_name] == cl].copy()
                    nb2keep = int(df[distrib_col_name].value_counts(normalize = True)[cl] * nb_data)
                data2add = buff_class.loc[random.sample(buff_class.index, \
                                                        nb2keep),:].copy()
                result = result.append(data2add)

    elif final_distrib == "EQUALLY":
        for cl in classes:
            buff_class = df[df[distrib_col_name] == cl].copy()
            data2add = buff_class.loc[random.sample(buff_class.index, nb_data_class),:].copy()
            result = result.append(data2add)
    
    elif final_distrib == "RANDOM":
        data2add = df.loc[random.sample(df.index, nb_data),:].copy()
        result = result.append(data2add) 
    
    # Some indexes may be shared between quantiles since we have
    # "discontinuous height" - integer ==> Thus remove duplicated indexes
    result["initial_index"] = result.index
    result.index = pd.Index(range(0,result.index.size))
    result = result.drop_duplicates(subset="initial_index")
    result.index = result["initial_index"]

    return result.reindex(result.index.drop_duplicates())
            
