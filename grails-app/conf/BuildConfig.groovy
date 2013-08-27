grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//location of the release repository
grails.project.repos.metridocRepo.url = "svn:https://metridoc.googlecode.com/svn/maven/repository"
//name of the repository
grails.project.repos.default = "metridocRepo"

grails.project.dependency.resolution = {


    grails.tomcat.jvmArgs = ["-Xmx768M", "-Xms768M", "-XX:PermSize=512m", "-XX:MaxPermSize=512m"]
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        grailsPlugins()
        grailsHome()
        mavenRepo "https://metridoc.googlecode.com/svn/maven/repository"
    }

    plugins {
        compile(":metridoc-core:0.7.1") {
            excludes "job-runner"
        }

        build ':release:2.2.1',
                ':rest-client-builder:1.0.3',
                ":tomcat:$grailsVersion",
                ':squeaky-clean:0.1.1',
                ':job-runner:0.6', {

            export = false
        }
    }
}
