grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.repos.metridocRepo.url = "https://metridoc.googlecode.com/svn/plugins"
grails.project.repos.default = "metridocRepo"

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
        compile (":quartz:1.0-RC2")
        compile (":metridoc-core:0.50") {
            exclude "xmlbeans"
            changing = true
        }
        build(":tomcat:$grailsVersion",
            ":release:2.0.3") {
            export = false
        }
    }
}
