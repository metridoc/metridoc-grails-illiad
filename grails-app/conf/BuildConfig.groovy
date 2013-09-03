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
        compile('com.github.metridoc:metridoc-job-core:0.7.6') {
            excludes 'slf4j-api'
            excludes 'jcl-over-slf4j'
        }
    }

    plugins {
        compile ':metridoc-core:0.7.2'
        runtime ':job-runner:0.6.1'

        build ':release:2.2.1',
                ':rest-client-builder:1.0.3',
                ":tomcat:$grailsVersion",
                ':squeaky-clean:0.1.1',
                ':bintray-upload:0.2', {
            export = false
        }
    }
}
