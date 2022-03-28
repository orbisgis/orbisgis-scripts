@GrabResolver(name='orbisgis', root='https://nexus.orbisgis.org/repository/orbisgis/')
@Grab(group='org.orbisgis', module='demat', version='0.0.6')
@Grab(group='org.orbisgis.orbisdata.datamanager', module='jdbc', version='1.0.1-SNAPSHOT')
@Grab(group='org.orbisgis.geoclimate', module='geoclimate', version='0.0.1')


import static org.orbisgis.demat.Plot.*
import org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS

H2GIS h2GIS = H2GIS.open("/tmp/demo_db")

// To make a graphic
h2GIS.execute("""DROP TABLE IF EXISTS TEST; CREATE TABLE TEST(ID int, TOTO int);
                INSERT INTO TEST VALUES (1,2), (2,15), (3,25);""")
             
Chart(Data(h2GIS.rows("SELECT * FROM TEST"))).mark_bar()
.encode(X("ID").nominal(), Y("TOTO").quantitative()).show()
                
// To make a map
h2GIS.execute("""DROP TABLE IF EXISTS TEST; CREATE TABLE TESTS AS SELECT st_makepoint(-60 + x*random()/500.00, 30 + x*random()/500.00) FROM GENERATE_SERIES(1, 200)""")
h2GIS.save("TEST", "/tmp/data.geojson")
             
Chart(Data(h2GIS.rows("SELECT * FROM TEST"))).mark_bar()
.encode(X("ID").nominal(), Y("TOTO").quantitative()).show()


