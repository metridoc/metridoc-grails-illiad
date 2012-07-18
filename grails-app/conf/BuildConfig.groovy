grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenCentral()
        grailsPlugins()
        grailsHome()
        grailsRepo "https://metridoc.googlecode.com/svn/plugins/"
    }

    plugins {
        compile (":metridoc-core:0.50-SNAPSHOT") {
            exclude "xmlbeans"
            changing = true
        }
        build(":tomcat:$grailsVersion",
            ":release:2.0.3") {
            export = false
        }
    }
}
