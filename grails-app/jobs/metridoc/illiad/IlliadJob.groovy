package metridoc.illiad

class IlliadJob extends Script {

    def dataSource_from_illiad
    def dataSource

    //TODO:need to fix the core so scripts can be called via the commandline easily instead of requiring this mess
    def execute(jobExecutionContext) {
        run()
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    @Override
    def run() {
        //TODO:need to get rid of this in the core.... just dump info in the binding or auto bind
        def jobDataMap = binding.jobDataMap
        String targetToRun
        if (jobDataMap) {
            targetToRun = jobDataMap.target
        }
        def tool = new IlliadTool()
        tool.setTargetToRun(targetToRun)
        tool.dataSource = dataSource
        tool.dataSource_from_illiad = dataSource_from_illiad
        tool.binding = binding
        tool.configure()
    }
}
