includeTargets << grailsScript("_GrailsBootstrap")

target(main: "main entry point into the illiad script") {
    depends(compile, createConfig)
    includeTargets << new File("scripts/_IlliadWorkflow.groovy")
    rootLoader.addURL(new File("${metridocIlliadPluginDir}/grails-app/conf").toURI().toURL())

    if(argsMap."do-bootstrap") {
        println "bootstrap enabled"
    }

    def cliParams = argsMap.params

    if(cliParams) {
        depends(cliParams)
    } else {
        depends(runIlliadWorkflow)
    }
}

setDefaultTarget(main)
