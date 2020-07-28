@Grab(group='tech.tablesaw', module='tablesaw-core', version='0.38.1')
@Grab(group='tech.tablesaw', module='tablesaw-jsplot', version='0.38.1')
    
@GrabResolver(name='orbisgis', root='http://nexus-ng.orbisgis.org/repository/orbisgis/')
@Grab(group='org.orbisgis.orbisdata.datamanager', module='jdbc', version='1.0.1-SNAPSHOT')

import tech.tablesaw.io.jdbc.*
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.ScatterPlot;


/**
* Example to use OrbisData and TableSaw to plot a chart
* See  : https://github.com/jtablesaw/tablesaw and https://github.com/orbisgis/orbisdata
* Author : Erwan Bocher, CNRS
**/

//Memory H2GIS database
def dbPath = "mem:"

H2GIS h2gis = H2GIS.open(dbPath);

h2gis.execute("""
                DROP TABLE IF EXISTS geo_table;
                CREATE TABLE geo_table as select st_makepoint(-60 + X*random()/500.00, 30 + X*random()/500.00) as the_geom, X as id from generate_series(1,100);
        """)
Table table = SqlResultSetReader.read(h2gis.getTable("(SELECT ST_X(the_geom) as x, st_Y(the_geom) as y from geo_table)"));

//The HTML file to save the plot
def outputFile = new File(System.getProperty("java.io.tmpdir") + File.separator+"plot.html")

Plot.show(
    ScatterPlot.create("X vs Y", 
                       table, "X", "Y"), outputFile);
                       
println("Data ploted in {outputFile.absolutePath()}")
