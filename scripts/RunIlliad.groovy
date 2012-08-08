includeTargets << grailsScript("_GrailsBootstrap")

target(main: "main entry point into the illiad script") {
    depends(compile, createConfig)
    includeTargets << new File("scripts/_IlliadWorkflow.groovy")
    rootLoader.addURL(new File("${metridocIlliadPluginDir}/grails-app/conf").toURI().toURL())

    if(argsMap."do-bootstrap") {
        println "bootstrap enabled"
    }

    def cliParams = argsMap.params
    def startDateParam = argsMap.startDate

    if(startDateParam) {
        startDate = startDateParam
        def isDate = startDate ==~ /\d{8}/
        if(!isDate) {
            def message = "startDate must be in the form 'yyyyMMdd', but was ${startDate}"
            throw new RuntimeException(message)
        }
    }

    if(cliParams) {
        depends(cliParams)
    } else {
        depends(runIlliadWorkflow)
    }
}

setDefaultTarget(main)
