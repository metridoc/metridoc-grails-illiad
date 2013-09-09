includeTargets << grailsScript("_GrailsBootstrap")

target(main: "main entry point into the illiad script") {
    grailsConsole.error "run-illiad is no longer supportted please so the job project here: https://github.com/metridoc/metridoc-job-illiad"
    exit(1)
}

setDefaultTarget(main)
