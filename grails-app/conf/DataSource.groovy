dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
/*    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'*/
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory'
}
environments {
    development {

            grails {
                mongo {
                    host = "localhost"
                    port = 27017
                    username = ""
                    password=""
                    databaseName = "newNest5DB"
                }
            }


        dataSource_trans {
            dbCreate = "update"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            url = "jdbc:postgresql://localhost:5432/newNest5"
            username = "postgres"
            password = "qtagtech"
        }


    }
    test {

        grails {
            mongo {
                host = System.env.OPENSHIFT_MONGODB_DB_HOST
                port = System.env.OPENSHIFT_MONGODB_DB_PORT
                username = "admin"
                password="YmIY6mDMasGt"
                databaseName = "ayJue"
            }
        }


        dataSource_trans {
            dbCreate = "update"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            host = System.env.OPENSHIFT_POSTGRESQL_DB_HOST
            port = System.env.OPENSHIFT_POSTGRESQL_DB_PORT
            url = "jdbc:postgresql://"+host+":"+port+"/"+System.env.OPENSHIFT_APP_NAME
            username = System.env.OPENSHIFT_POSTGRESQL_DB_USERNAME
            password = System.env.OPENSHIFT_POSTGRESQL_DB_PASSWORD
        }


    }
    production {
        grails {
            mongo {
                host = System.env.OPENSHIFT_MONGODB_DB_HOST
                port = System.env.OPENSHIFT_MONGODB_DB_PORT
                username = "admin"
                password="YmIY6mDMasGt"
                databaseName = "ayJue"
            }
        }
        dataSource_trans {
            dbCreate = "update"
            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            host = System.env.OPENSHIFT_POSTGRESQL_DB_HOST
            port = System.env.OPENSHIFT_POSTGRESQL_DB_PORT
            url = "jdbc:postgresql://"+host+":"+port+"/"+System.env.OPENSHIFT_APP_NAME
            username = System.env.OPENSHIFT_POSTGRESQL_DB_USERNAME
            password = System.env.OPENSHIFT_POSTGRESQL_DB_PASSWORD
        }

    }
}
