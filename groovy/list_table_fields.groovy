ds = mc.getDataManager().getDataSource()
// Get an SQL object to work with
sql = groovy.sql.Sql.newInstance(ds)
def colNames = []
sql.cacheConnection {
    // catalog,schema,table,columnNamePattern
    colsRs = sql.connection.metaData.getColumns(null,null,"MYTABLE",null)
    while (colsRs.next()) {
        colNames << colsRs.getString('column_name')
    }
} 
print colNames
