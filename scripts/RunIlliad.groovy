includeTargets << grailsScript("_GrailsBootstrap")

target(main: "main entry point into the illiad script") {
    grailsConsole.error "run-illiad script is no longer supported, please run 'grails run-job illiad' instead"
    exit(1)
}

setDefaultTarget(main)
