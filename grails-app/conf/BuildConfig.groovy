grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.repos.metridocRepo.url = "https://api.bintray.com/maven/upennlib/metridoc/metridoc-illiad"
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
        //TODO: more up to date versions of the core do not require the exclusions.  remove once we update the core
        compile ':metridoc-core:0.7.2'
        runtime ':job-runner:0.6.1'

        build ':release:2.2.1',
                ':rest-client-builder:1.0.3',
                ":tomcat:$grailsVersion",
                ':squeaky-clean:0.1.1', {
            export = false
        }
    }
}
