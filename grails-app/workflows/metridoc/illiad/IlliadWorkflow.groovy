package metridoc.illiad

import groovy.sql.Sql
import metridoc.camel.MetridocSimpleRegistry

class IlliadWorkflow extends Script {

    def dataSource_from_illiad
    def dataSource_illiad
    def grailsApplication
    def illiadWorkflowService
    def illiadService
    Sql illiadFromSql
    Sql illiadDestinationSql

    @Override
    Object run() {

        def illiadConfig = grailsApplication.mergedConfig.metridoc.illiad
        def startDate = illiadConfig.startDate

        target(runIlliad: "runs the illiad workflow") {
            log.info "running illiad starting at ${new Date()}"

            depends(
                    createSqlClasses,
                    cacheViewDataIfItExists,
                    clearingIlliadTables,
                    migrateData,
                    doUpdateBorrowing,
                    doUpdateLending,
                    doUpdateDemographics
            )
            //call directly since depends only runs once
            cacheViewDataIfItExists.call()
        }

        target(cacheViewDataIfItExists: "caches data if it exists before running") {
            depends(createSqlClasses)
            def total = illiadDestinationSql.firstRow("select count(*) as total from ill_group").total

            if(total) {
                illiadService.populateModel()
            }
        }

        target(createSqlClasses: "creates instances of groovy.sql.Sql classes that will perform raw sql against illiad and the repository") {
            illiadFromSql = new Sql(dataSource_from_illiad)
            illiadDestinationSql = new Sql(dataSource_illiad)
        }

        target(clearingIlliadTables: "truncates all tables") {
            depends(createSqlClasses)
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
                depends(createSqlClasses)
                prepareClosure(illiadConfig.updateBorrowing).call(illiadDestinationSql, illiadConfig)
            }
        }

        target(doUpdateLending: "updates lending tables in the destination data source") {
            profile("updating illiad lending"){
                depends(createSqlClasses)
                prepareClosure(illiadConfig.updateLending).call(illiadDestinationSql, illiadConfig)
            }
        }

        target(doUpdateDemographics: "updating demographic information") {
            profile("updating illiad demographics"){
                depends(createSqlClasses)
                prepareClosure(illiadConfig.updateDemographics).call(illiadDestinationSql, illiadConfig)
            }
        }
    }

    def prepareClosure(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST

        return closure
    }
}


