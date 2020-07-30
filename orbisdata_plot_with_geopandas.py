#!/usr/bin/python3
# -*- coding: utf-8 -*-

import geopandas as gpd
import matplotlib.pyplot as plt

# Data location
dirfile = "/home/erenault/geoclimate/osm/output/osm_vannes/"  

# Example of plot for a geojson variable.
plt.rcParams['figure.figsize'] = (20, 10)
data = gpd.read_file(dirfile+"block_indicators.geojson")
ax = data.plot(color='blue')
data.plot(ax=ax, column="hole_area_density", cmap='OrRd')
plt.show()
