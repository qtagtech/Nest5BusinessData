dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
environments {
    development {

            grails {
                mongo {
                    host = "localhost"
                    port = 27017
                    username = ""
                    password=""
                    databaseName = "nest5BigData"
                }
            }


        dataSource_trans {
            //dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            //url = "jdbc:h2:mem:devDb;MVCC=TRUE"
            dbCreate = "update"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            url = "jdbc:postgresql://localhost:5432/nest5BigDataTrans"
            username = "postgres"
            password = "qtagtech"
        }


    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    production {
        grails {
            mongo {
                host = System.env.OPENSHIFT_MONGODB_DB_HOST
                port = System.env.OPENSHIFT_MONGODB_DB_PORT
                username = "admin"
                password="YmIY6mDMasGt"
                databaseName = "bigdata"
            }
        }

    }
}
