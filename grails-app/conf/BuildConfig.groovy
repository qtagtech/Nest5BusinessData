grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.7
grails.project.source.level = 1.7
grails.project.war.file = "target/ROOT.war"

grails.project.fork = [
        // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
        //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

        // configure settings for the test-app JVM, uses the daemon by default
        test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
        // configure settings for the run-app JVM
        run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the run-war JVM
        war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the Console UI JVM
        console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenRepo "http://repo.grails.org/grails/plugins"
        mavenLocal()
        mavenCentral()
        mavenRepo "http://repository.springsource.com/maven/bundles/release"
        mavenRepo "http://repository.springsource.com/maven/bundles/external"
        mavenRepo "http://repo.spring.io/milestone/"
        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        mavenRepo "http://snapshots.repository.codehaus.org"
        mavenRepo "http://repository.codehaus.org"
        mavenRepo "http://download.java.net/maven/2/"
        mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
        runtime 'org.postgresql:postgresql:9.2-1004-jdbc4'
        compile 'org.apache.httpcomponents:httpcore:jar:4.3.1'
        compile 'org.apache.httpcomponents:httpclient:jar:4.3.1'
        compile 'org.codehaus.jackson:jackson-core-asl:1.8.7'
        compile 'org.codehaus.jackson:jackson-mapper-asl:1.8.1'
        compile 'com.amazonaws:aws-java-sdk:1.5.0'
        compile 'org.eclipse.birt.runtime.3_7_1:org.apache.commons.codec:jar:1.3.0'
        compile 'joda-time:joda-time:2.3'
        compile 'commons-logging:commons-logging:jar:1.1'
        compile 'org.grails:grails-datastore-gorm:3.0.4.RELEASE'
        compile 'org.grails:grails-datastore-core:3.0.4.RELEASE'
        test 'org.grails:grails-datastore-simple:3.0.4.RELEASE'
        compile('org.codehaus.groovy.modules.http-builder:http-builder:jar:0.5.0-RC2') {
            excludes "commons-logging", "xml-apis", "groovy","httpclient","httpcore"
        }

        // runtime 'mysql:mysql-connector-java:5.1.20'
    }

    plugins {
        build ':tomcat:7.0.53'
        compile ":hibernate4:4.3.5.4"
        compile ':mongodb:3.0.1'
      runtime ":resources:1.2.1"
        runtime ":resources:1.2.1"
        compile ":spring-security-core:2.0-RC3"
   }


}
