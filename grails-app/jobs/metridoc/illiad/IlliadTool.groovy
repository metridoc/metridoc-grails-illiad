package metridoc.illiad

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import metridoc.core.MetridocScript
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import metridoc.utils.DateUtil

import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 4/1/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
class IlliadTool extends RunnableTool {

    static final LENDER_ADDRESSES_ALL = "LenderAddressesAll"
    static final LENDER_ADDRESSES = "LenderAddresses"
    static final USERS = "Users"
    static final USERS_ALL = "UsersAll"
    public static final String OTHER = 'Other'

    def _lenderTableName
    def _userTableName
    String targetToRun = "default"
    String startDate
    Sql sql
    Sql fromIlliadSql
    DataSource dataSource
    DataSource dataSource_from_illiad
    List<Class<Script>> targetClasses = []
    Script illiadSqlStatements

    @Override
    void setBinding(Binding binding) {
        super.setBinding(binding)
        use(MetridocScript) {
            binding.includeTool(CamelTool)
        }
    }

    Script getIlliadSqlStatements() {
        if (illiadSqlStatements) return illiadSqlStatements

        illiadSqlStatements = new IlliadSql()
        illiadSqlStatements.run()

        illiadSqlStatements
    }

    Sql getSql() {
        if (sql) return sql
        sql = new Sql(dataSource)
    }

    @Override
    def doRun() {
        def binding = getBinding()
        addDefaultTargets()
        use(MetridocScript) {
            targetClasses.each { Class<Script> targets ->
                binding.includeTargets(targets)
            }

            MetridocScript.getManager(binding).depends(targetToRun)
        }
    }

    def addDefaultTargets() {
        addClearingTablesTarget()
        addMigrateDataTarget()
        addBorrowingUpdate()
        addLendingUpdate()
        addIllGroupOtherInsert()
        addCleanUpIllTransactionLendingLibraries()

        use(MetridocScript) {
            binding.target(default: "runs illiad workflow") {
                depends("clearingIlliadTables", "migrateData", "doUpdateBorrowing", "doUpdateLending", "doIllGroupOtherInsert", "cleanUpIllTransactionLendingLibraries")
            }
        }
    }

    void addCleanUpIllTransactionLendingLibraries() {
        use(MetridocScript){
            binding.target(cleanUpIllTransactionLendingLibraries: "cleans up data in ill_transaction to facilitate agnostic sql queries in the dashboard"){
                getSql().execute("update ill_transaction set lending_library = 'Other' where lending_library is null")
                getSql().execute("update ill_transaction set lending_library = 'Other' where lending_library not in (select distinct lender_code from ill_lender_group)")
            }
        }
    }

    void addIllGroupOtherInsert() {
        use(MetridocScript){
            binding.target(doIllGroupOtherInsert: "inserts extra records into ill_group to deal with 'OTHER'"){
                IllGroup.withNewTransaction {
                    new IllGroup(groupNo: IlliadService.GROUP_ID_OTHER, groupName: OTHER).save(failOnError: true)
                    new IllLenderGroup(groupNo: IlliadService.GROUP_ID_OTHER, lenderCode: OTHER).save(failOnError: true)
                }
            }
        }
    }

    void addLendingUpdate() {
        use(MetridocScript){
            binding.target(doUpdateLending: "updates the lending table"){
                [
                        getIlliadSqlStatements().orderDateSqlStmt,
                        getIlliadSqlStatements().shipDateSqlStmt,
                        getIlliadSqlStatements().receiveDateSqlStmt,
                        getIlliadSqlStatements().articleReceiveDateSqlStmt
                ].each {
                    log.info "updating lending with sql statement $it"
                    getSql().execute(it as String)
                }
            }
        }
    }

    void addBorrowingUpdate() {
        use(MetridocScript) {
            binding.target(doUpdateBorrowing: "updates the borrowing tables") {
                IllTracking.updateFromIllBorrowing()
                [
                        getIlliadSqlStatements().orderDateSqlStmt,
                        getIlliadSqlStatements().shipDateSqlStmt,
                        getIlliadSqlStatements().receiveDateSqlStmt,
                        getIlliadSqlStatements().articleReceiveDateSqlStmt
                ].each {
                    log.info "update borrowing with sql statement $it"
                    getSql().execute(it as String)
                }
            }
        }
    }

    void addMigrateDataTarget() {

        use(MetridocScript) {
            CamelTool camelTool = binding.camelTool
            binding.dataSource = dataSource
            binding.dataSource_from_illiad = dataSource_from_illiad
            binding.target(migrateData: "migrates data from illiad to repository instance") {
                [
                        ill_group: getIlliadSqlStatements().groupSqlStmt,
                        ill_lender_group: getIlliadSqlStatements().groupLinkSqlStmt,
                        ill_lender_info: getIlliadSqlStatements().lenderAddrSqlStmt(lenderTableName),
                        ill_reference_number: getIlliadSqlStatements().referenceNumberSqlStmt,
                        ill_transaction: getIlliadSqlStatements().transactionSqlStmt(getStartDate()()),
                        ill_lending: getIlliadSqlStatements().lendingSqlStmt(getStartDate()()),
                        ill_borrowing: getIlliadSqlStatements().borrowingSqlStmt(getStartDate()()),
                        ill_user_info: getIlliadSqlStatements().userSqlStmt(userTableName)

                ].each { key, value ->
                    log.info("migrating to ${key} using \n    ${value}" as String)
                    camelTool.consumeNoWait("sqlplus:${value}?dataSource=dataSource_from_illiad") { ResultSet resultSet ->
                        camelTool.send("sqlplus:${key}?dataSource=dataSource", resultSet)
                    }
                }
            }
        }
    }

    String getStartDate() {
        if (startDate) return startDate

        def formatter = new SimpleDateFormat('yyyyMMdd')
        def fiscalYear = DateUtil.currentFiscalYear
        def startDateAsDate = DateUtil.getFiscalYearStartDate(fiscalYear)

        startDate = formatter.format(startDateAsDate)
    }

    void addClearingTablesTarget() {
        use(MetridocScript) {
            binding.target(clearingIlliadTables: "truncates all tables") {
                [
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
                ].each {
                    log.info "truncating table ${it} in the repository"
                    getSql().execute("truncate ${it}" as String)
                }
            }
        }
    }

    def getFromIlliadSql() {
        if (fromIlliadSql) return fromIlliadSql

        fromIlliadSql = new Sql(dataSource_from_illiad)
    }

    def getLenderTableName() {
        if (_lenderTableName) return _lenderTableName

        _lenderTableName = pickTable(LENDER_ADDRESSES_ALL, LENDER_ADDRESSES)
    }

    def getUserTableName() {
        if (_userTableName) return _userTableName

        _userTableName = pickTable(USERS, USERS_ALL)
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
        } catch (SQLException e) {
            //table does not exist
            return false
        }
    }
}
