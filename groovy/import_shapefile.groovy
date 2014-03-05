import org.h2gis.drivers.shp.SHPDriverFunction
import org.h2gis.h2spatialapi.EmptyProgressVisitor

ds = mc.getDataManager().getDataSource()
// Get an SQL object to work with
sql = groovy.sql.Sql.newInstance(ds)

def driver = new SHPDriverFunction()
sql.cacheConnection {
    driver.importFile(sql.connection, "myschema.outputtable", new File("/home/user/dataSterenn/landcover2000.shp"),
    new EmptyProgressVisitor())
}
