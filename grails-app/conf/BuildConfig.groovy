grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//location of the release repository
grails.project.repos.metridocRepo.url = "svn:https://metridoc.googlecode.com/svn/maven/repository"
//name of the repository
grails.project.repos.default = "metridocRepo"

grails.project.dependency.resolution = {
    inherits("global")
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        //TODO: get rid of this once we have locked down versions
        mavenLocal()
        mavenCentral()
        mavenRepo "http://dl.bintray.com/upennlib/metridoc"
        mavenRepo "http://dl.bintray.com/upennlib/maven"
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
