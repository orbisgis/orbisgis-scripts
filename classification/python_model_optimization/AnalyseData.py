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
                     uniqueness_threshold = 0.9, uniqueness_col_name = "UNIQUENESS_VALUE"):
    """ Select a random sample of 'nb_data' (number of building) respecting the 
    'final_distribution' of 'distrib_col_name' (building typology).
    
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
            uniqueness_threshold : float, default 0.9
                We conserve only uniqueness values > uniqueness_threshold. The ratio of uniqueness
                is an accuracy indicator assigned to an object (closer to 1, better the accuracy)
            uniqueness_col_name : string, default "UNIQUENESS_VALUE"
                Name of the uniqueness column
        Returns
    _ _ _ _ _ _ _ _ _ _ 
    
            The selected data (pd.DataFrame object)"""
    result = pd.DataFrame(columns = df.columns)
    
    buff = df[df[uniqueness_col_name] >= uniqueness_threshold]

    if final_distrib != "RANDOM":
        classes = sorted(set(buff[distrib_col_name]))
    
    if final_distrib == "REPRESENTATIVE":
        for cl in classes:
            buff_class = buff[buff[distrib_col_name] == cl].copy()
            nb2keep = int(buff[distrib_col_name].value_counts(normalize = True)[cl] * nb_data)
            data2add = buff_class.loc[random.sample(buff_class.index, \
                                                    nb2keep),:].copy()
            result = result.append(data2add)

    elif final_distrib == "EQUALLY":
        for cl in classes:
            buff_class = buff[buff[distrib_col_name] == cl].copy()
            data2add = buff_class.loc[random.sample(buff_class.index, nb_data_class),:].copy()
            result = result.append(data2add)
    
    elif final_distrib == "RANDOM":
        data2add = buff.loc[random.sample(buff.index, nb_data),:].copy()
        result = result.append(data2add) 
    
    return result
            
