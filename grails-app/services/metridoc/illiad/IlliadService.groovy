package metridoc.illiad

import groovy.sql.Sql
import metridoc.core.MetridocJob
import metridoc.utils.DateUtil

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat

class IlliadService extends MetridocJob {

    def dataSource_from_illiad
    def dataSource
    def illiadReportingService
    Sql sql
    Sql fromIlliadSql
    IlliadMsSqlQueries illiadSqlStatements = new IlliadMsSqlQueries()
    static final LENDER_ADDRESSES_ALL = "LenderAddressesAll"
    static final LENDER_ADDRESSES = "LenderAddresses"
    static final USERS = "Users"
    static final USERS_ALL = "UsersAll"
    public static final String OTHER = "Other"

    def _lenderTableName
    def _userTableName
    String startDate
    def illiadTables = [
            "ill_group",
            "ill_lending",
            "ill_borrowing",
            "ill_user_info",
            "ill_transaction",
            "ill_lender_info",
            "ill_lender_group",
            "ill_lending_tracking",
            "ill_location",
            "ill_reference_number",
            "ill_tracking"
    ]

    @Override
    def configure() {

        target(runWorkflow: "runs the full illiad workflow") {
            def migratingFromUrl = (dataSource_from_illiad as DataSource).connection.metaData.getURL()
            def migratingToUrl = (dataSource as DataSource).connection.metaData.getURL()
            log.info "migrating from $migratingFromUrl to $migratingToUrl"

            depends("clearingIlliadTables",
                    "migrateData",
                    "migrateBorrowingDataToIllTracking",
                    "doUpdateBorrowing",
                    "doUpdateLending",
                    "doIllGroupOtherInsert",
                    "cleanUpIllTransactionLendingLibraries")
        }

        target(clearingIlliadTables: "truncates all tables") {
            illiadTables.each {
                log.info "truncating table ${it} in the repository"
                getSql().execute("truncate ${it}" as String)
            }
        }

        target(migrateData: "migrates data from illiad to repository instance") {
            //adding variables to the binding makes them accessible to the camel tool
            binding.dataSource = dataSource
            binding.dataSource_from_illiad = dataSource_from_illiad
            [
                    ill_group: illiadSqlStatements.groupSqlStmt,
                    ill_lender_group: illiadSqlStatements.groupLinkSqlStmt,
                    ill_lender_info: illiadSqlStatements.lenderAddrSqlStmt(lenderTableName as String),
                    ill_reference_number: illiadSqlStatements.referenceNumberSqlStmt,
                    ill_transaction: illiadSqlStatements.transactionSqlStmt(getStartDate()),
                    ill_lending: illiadSqlStatements.lendingSqlStmt(getStartDate()),
                    ill_borrowing: illiadSqlStatements.borrowingSqlStmt(getStartDate()),
                    ill_user_info: illiadSqlStatements.userSqlStmt(userTableName as String)

            ].each { key, value ->
                log.info("migrating to ${key} using \n    ${value}" as String)
                consumeNoWait("sqlplus:${value}?dataSource=dataSource_from_illiad") { ResultSet resultSet ->
                    send("sqlplus:${key}?dataSource=dataSource", resultSet)
                }
            }
        }

        target(migrateBorrowingDataToIllTracking: "migrates data from illborrowing to ill_tracking") {
            IllTracking.updateFromIllBorrowing()
        }

        target(doUpdateBorrowing: "updates the borrowing tables") {
            [
                    illiadSqlStatements.orderDateSqlStmt,
                    illiadSqlStatements.shipDateSqlStmt,
                    illiadSqlStatements.receiveDateSqlStmt,
                    illiadSqlStatements.articleReceiveDateSqlStmt
            ].each {
                log.info "update borrowing with sql statement $it"
                getSql().execute(it as String)
            }
        }

        target(doUpdateLending: "updates the lending table") {
            [
                    illiadSqlStatements.arrivalDateSqlStmt,
                    illiadSqlStatements.completionSqlStmt,
                    illiadSqlStatements.cancelledSqlStmt
            ].each {
                log.info "updating lending with sql statement $it"
                getSql().execute(it as String)
            }
        }

        target(doIllGroupOtherInsert: "inserts extra records into ill_group to deal with 'OTHER'") {
            IllGroup.withNewTransaction {
                new IllGroup(groupNo: IlliadReportingService.GROUP_ID_OTHER, groupName: OTHER).save(failOnError: true)
                new IllLenderGroup(groupNo: IlliadReportingService.GROUP_ID_OTHER, lenderCode: OTHER).save(failOnError: true)
            }
        }

        target(cleanUpIllTransactionLendingLibraries: "cleans up data in ill_transaction, ill_lending_tracking and ill_tracking to facilitate agnostic sql queries in the dashboard") {

            getSql().withTransaction {
                int updates
                updates = getSql().executeUpdate("update ill_transaction set lending_library = 'Other' where lending_library is null")
                log.info "changing all lending_library entries in ill_transaction from null to other caused $updates updates"
                updates = getSql().executeUpdate("update ill_transaction set lending_library = 'Other' where lending_library not in (select distinct lender_code from ill_lender_group)")
                log.info "changing all lending_library entries in ill_transaction that are not in ill_lender_group to other caused $updates updates"
            }

            IllTracking.updateTurnAroundsForAllRecords()
            IllLendingTracking.updateTurnAroundsForAllRecords()
            if (illiadReportingService) {
                illiadReportingService.storeCache()
            }
        }

        target(dropTables: "drops illiad tables") {
            illiadTables.each {
                getSql().execute("drop table $it" as String)
            }
        }

        setDefaultTarget("runWorkflow")
    }

    Sql getSql() {
        if (sql) return sql
        sql = new Sql(dataSource as DataSource)
    }

    def getLenderTableName() {
        if (_lenderTableName) return _lenderTableName

        _lenderTableName = pickTable(LENDER_ADDRESSES_ALL, LENDER_ADDRESSES)
    }

    def getUserTableName() {
        if (_userTableName) return _userTableName

        _userTableName = pickTable(USERS, USERS_ALL)
    }

    String getStartDate() {
        if (startDate) return startDate

        def formatter = new SimpleDateFormat('yyyyMMdd')
        def fiscalYear = DateUtil.currentFiscalYear
        def startDateAsDate = DateUtil.getFiscalYearStartDate(fiscalYear)

        startDate = formatter.format(startDateAsDate)
    }

    private pickTable(option1, option2) {
        if (tableExists(option1)) {
            return option1
        } else {
            return option2
        }
    }

    private tableExists(tableName) {
        try {
            getFromIlliadSql().execute("select count(*) from $tableName" as String)
            return true
        } catch (SQLException ignored) {
            //table does not exist
            return false
        }
    }

    def getFromIlliadSql() {
        if (fromIlliadSql) return fromIlliadSql

        fromIlliadSql = new Sql(dataSource_from_illiad as DataSource)
    }
}
