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
        mavenCentral()
        mavenRepo "http://dl.bintray.com/upennlib/metridoc"
        mavenRepo "http://dl.bintray.com/upennlib/maven"
    }

    dependencies {
        compile 'net.sf.opencsv:opencsv:2.3'
    }

    plugins {
        compile ':metridoc-core:0.7.7'

        build ':release:2.2.1',
                ':rest-client-builder:1.0.3',
                ":tomcat:$grailsVersion",
                ':squeaky-clean:0.1.1',
                ':bintray-upload:0.2', {
            export = false
        }
    }
}
