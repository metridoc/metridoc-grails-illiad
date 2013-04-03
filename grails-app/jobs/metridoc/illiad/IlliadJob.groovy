package metridoc.illiad

import grails.util.Holders
import groovy.sql.Sql
import metridoc.core.MetridocJob

class IlliadJob extends MetridocJob {
    static triggers = {
        def configuredSchedule = Holders.grailsApplication?.mergedConfig.metridoc.illiad.schedule
        def scheduleUsed = configuredSchedule ?: "0 0 0 * * ?"
        cron name: "illiad ingestor", cronExpression: scheduleUsed
    }

    def dataSource_from_illiad
    def dataSource

    @Override
    def doExecute() {
        def tool = new IlliadTool()
        tool.dataSource = dataSource
        tool.dataSource_from_illiad = dataSource_from_illiad
        tool.binding = binding
        tool.doRun()
    }
}
