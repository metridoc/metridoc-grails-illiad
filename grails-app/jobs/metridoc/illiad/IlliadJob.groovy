package metridoc.illiad

import metridoc.core.MetridocJob
import grails.util.Holders
import groovy.sql.Sql

class IlliadJob extends MetridocJob {
    static triggers = {
        def configuredSchedule = Holders.grailsApplication?.mergedConfig.metridoc.illiad.schedule
        def scheduleUsed = configuredSchedule ?: "0 0 0 * * ?"
        cron name:  "illiad ingestor", cronExpression: scheduleUsed
    }

    Sql _illiadFromSql
    Sql _illiadDestinationSql
    def dataSource_from_illiad
    def dataSource_illiad
    def illiadService
    def grailsApplication
    def illiadWorkflowService

    @Override
    def doExecute() {
        def illiadConfig = grailsApplication.mergedConfig.metridoc.illiad
        def startDate = illiadConfig.startDate

        target(default: "full illiad workflow") {
            profile("running full illiad workflow") {
                depends(
                        "cacheViewDataIfItExists",
                        "clearingIlliadTables",
                        "migrateData",
                        "doUpdateBorrowing",
                        "doUpdateLending",
                        "doUpdateDemographics"
                )
            }
        }

        target(cacheViewDataIfItExists: "caches data if it exists before running") {
            def total = illiadDestinationSql.firstRow("select count(*) as total from ill_group").total

            if(total) {
                illiadService.populateModel()
            }
        }

        target(clearingIlliadTables: "truncates all tables") {
            profile("truncating illiad tables") {
                prepareClosure(illiadConfig.truncateIlliadTablesInRepository).call(illiadDestinationSql)
            }
        }

        target(migrateData: "migrates data from illiad to local mysql instance") {

            log.info "beginning migration from illiad to repository"

            def lenderTable = illiadWorkflowService.lenderTableName
            def userTable = illiadWorkflowService.userTableName

            profile("illiad full migration") {
                [
                        ill_group: illiadConfig.groupSqlStmt,
                        ill_lender_group: illiadConfig.groupLinkSqlStmt,
                        ill_lender_info: illiadConfig.lenderAddrSqlStmt(lenderTable),
                        ill_reference_number: illiadConfig.referenceNumberSqlStmt,
                        ill_transaction: illiadConfig.transactionSqlStmt(startDate),
                        ill_lending: illiadConfig.lendingSqlStmt(startDate),
                        ill_borrowing: illiadConfig.borrowingSqlStmt(startDate),
                        ill_user_info: illiadConfig.userSqlStmt(userTable)

                ].each {key, value ->
                    log.info("migrating to ${key} using \n    ${value}" as String)
                    profile("migration ${key}") {
                        runRoute {
                            from("sqlplus:${value}?dataSource=dataSource_from_illiad").to("sqlplus:${key}?dataSource=dataSource_illiad")
                        }
                    }
                }
            }
        }

        target(doUpdateBorrowing: "updates the borrowing tables") {
            profile("updating illiad borrowing"){
                prepareClosure(illiadConfig.updateBorrowing).call(illiadDestinationSql, illiadConfig)
            }
        }

        target(doUpdateLending: "updates lending tables in the destination data source") {
            profile("updating illiad lending"){
                prepareClosure(illiadConfig.updateLending).call(illiadDestinationSql, illiadConfig)
            }
        }

        target(doUpdateDemographics: "updating demographic information") {
            profile("updating illiad demographics"){
                prepareClosure(illiadConfig.updateDemographics).call(illiadDestinationSql, illiadConfig)
            }
        }
    }

    Sql getIlliadFromSql() {
        if(_illiadFromSql) return _illiadFromSql

        _illiadFromSql = new Sql(dataSource_from_illiad)
    }

    Sql getIlliadDestinationSql() {
        if (_illiadDestinationSql) return _illiadDestinationSql

        _illiadDestinationSql = new Sql(dataSource_illiad)
    }

    def prepareClosure(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST

        return closure
    }
}
