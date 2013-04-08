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
    public static final String OTHER = "Other"

    def _lenderTableName
    def _userTableName
    String targetToRun = "default"
    String startDate
    Sql sql
    Sql fromIlliadSql
    DataSource dataSource
    DataSource dataSource_from_illiad
    List<Class<Script>> targetClasses = []
    IlliadMsSqlQueries illiadSqlStatements = new IlliadMsSqlQueries()

    void setTargetToRun(String targetToRun) {
        if (targetToRun) {
            this.targetToRun = targetToRun
        }
    }

    @Override
    void setBinding(Binding binding) {
        super.setBinding(binding)
        use(MetridocScript) {
            binding.includeTool(CamelTool)
        }
    }

    Sql getSql() {
        if (sql) return sql
        sql = new Sql(dataSource)
    }

    @Override
    def configure() {
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
        use(MetridocScript) {
            binding.target(cleanUpIllTransactionLendingLibraries: "cleans up data in ill_transaction, ill_lending_tracking and ill_tracking to facilitate agnostic sql queries in the dashboard") {
                getSql().execute("update ill_transaction set lending_library = 'Other' where lending_library is null")
                getSql().execute("update ill_transaction set lending_library = 'Other' where lending_library not in (select distinct lender_code from ill_lender_group)")
                IllTracking.updateTurnAroundsForAllRecords()
                IllLendingTracking.updateTurnAroundsForAllRecords()
            }
        }
    }

    void addIllGroupOtherInsert() {
        use(MetridocScript) {
            binding.target(doIllGroupOtherInsert: "inserts extra records into ill_group to deal with 'OTHER'") {
                IllGroup.withNewTransaction {
                    new IllGroup(groupNo: IlliadService.GROUP_ID_OTHER, groupName: OTHER).save(failOnError: true)
                    new IllLenderGroup(groupNo: IlliadService.GROUP_ID_OTHER, lenderCode: OTHER).save(failOnError: true)
                }
            }
        }
    }

    void addBorrowingUpdate() {
        use(MetridocScript) {
            binding.target(doUpdateBorrowing: "updates the borrowing tables") {
                IllTracking.updateFromIllBorrowing()
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
        }
    }

    void addLendingUpdate() {
        use(MetridocScript) {
            binding.target(doUpdateLending: "updates the lending table") {
                [
                        illiadSqlStatements.arrivalDateSqlStmt,
                        illiadSqlStatements.completionSqlStmt,
                        illiadSqlStatements.cancelledSqlStmt
                ].each {
                    log.info "updating lending with sql statement $it"
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
                        ill_group: illiadSqlStatements.groupSqlStmt,
                        ill_lender_group: illiadSqlStatements.groupLinkSqlStmt,
                        ill_lender_info: illiadSqlStatements.lenderAddrSqlStmt(lenderTableName),
                        ill_reference_number: illiadSqlStatements.referenceNumberSqlStmt,
                        ill_transaction: illiadSqlStatements.transactionSqlStmt(getStartDate()),
                        ill_lending: illiadSqlStatements.lendingSqlStmt(getStartDate()),
                        ill_borrowing: illiadSqlStatements.borrowingSqlStmt(getStartDate()),
                        ill_user_info: illiadSqlStatements.userSqlStmt(userTableName)

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

class IlliadMsSqlQueries {

    def groupSqlStmt = "select distinct GroupNumber as group_no, GroupName as group_name from Groups"

    def groupLinkSqlStmt = "select distinct GroupNumber as group_no, LenderString as lender_code from GroupsLink"

    def lenderAddrSqlStmt = { String lenderTableName ->
        "select distinct LenderString as lender_code, LibraryName as library_name, " +
                " BillingCategory as billing_category, address1+'; '+address2+'; '+address3+'; '+address4 as address " +
                " from ${lenderTableName}"
    }

    def referenceNumberSqlStmt = "select distinct i.TransactionNumber as transaction_number, i.OCLCNumber as oclc, " +
            " i.Type as ref_type, i.Data as ref_number from WorldCatInformation i, Transactions t " +
            " where t.TransactionNumber = i.TransactionNumber and t.TransactionStatus in ('Request Finished','Cancelled by ILL Staff')"

    def transactionSqlStmt = { String startDate ->
        "select TransactionNumber as transaction_number, " +
                " SUBSTRING(sys.fn_sqlvarbasetostr(HASHBYTES('MD5',UserName)),3,32) as user_id, RequestType as request_type, " +
                " LoanAuthor as loan_author, LoanTitle as loan_title, LoanPublisher as loan_publisher, LoanPlace as loan_location, " +
                " LoanDate as loan_date, LoanEdition as loan_edition, PhotoJournalTitle as photo_journal_title, " +
                " PhotoJournalVolume as photo_journal_volume, PhotoJournalIssue as photo_journal_issue, " +
                " PhotoJournalMonth as photo_journal_month, PhotoJournalYear as photo_journal_year, " +
                " PhotoJournalInclusivePages as photo_journal_inclusive_pages, PhotoArticleAuthor as photo_article_author, " +
                " PhotoArticleTitle as photo_article_title, CitedIn as cited_in, TransactionStatus as transaction_status, " +
                " TransactionDate as transaction_date, ISSN, ESPNumber as ESP_number, LendingString as lender_codes, " +
                " LendingLibrary as lending_library, ReasonForCancellation as reason_for_cancellation, CallNumber as call_number, " +
                " Location as location, ProcessType as process_type, SystemID as system_id, IFMCost as IFM_cost, " +
                " InProcessDate as in_process_date, BillingAmount as billing_amount from Transactions " +
                " where TransactionStatus in ('Request Finished','Cancelled by ILL Staff') and convert(varchar(11), TransactionDate, 112) >= '${startDate}'"
    }

    def lendingSqlStmt = { String startDate ->
        "select t2.TransactionNumber as transaction_number, t1.RequestType as request_type, t2.ChangedTo as status, min(t2.DateTime) as transaction_date " +
                " from Transactions t1 join Tracking t2 on t2.TransactionNumber = t1.TransactionNumber and t1.ProcessType = 'Lending' " +
                " where convert(varchar(11), t1.TransactionDate, 112) >= '${startDate}' and " +
                " (t1.RequestType = 'Article' and t2.ChangedTo in ('Awaiting Lending Request Processing','Request Finished','Request Conditionalized','Cancelled by ILL Staff') or " +
                " t1.RequestType = 'Loan' and t2.ChangedTo in ('Awaiting Lending Request Processing','Awaiting Mailing', 'Item Shipped','Request Conditionalized','Cancelled by ILL Staff')) " +
                " group by t2.TransactionNumber, t1.RequestType, t2.ChangedTo"
    }

    def borrowingSqlStmt = { String startDate ->
        "select t2.TransactionNumber as transaction_number, t1.RequestType as request_type, " +
                " t2.ChangedTo as transaction_status, min(t2.DateTime) as transaction_date " +
                " from Transactions t1 join Tracking t2 on t2.TransactionNumber=t1.TransactionNumber " +
                " where t1.ProcessType = 'Borrowing' and t1.TransactionStatus in ('Request Finished','Request Conditionalized','Cancelled by ILL Staff') and " +
                " t2.ChangedTo in ('Awaiting Copyright Clearance','Awaiting Request Processing','Request Sent','Awaiting Post Receipt Processing','Delivered to Web') and " +
                " convert(varchar(11), t1.TransactionDate, 112) >= '${startDate}' " +
                " group by t2.TransactionNumber, t1.RequestType, t2.ChangedTo " +
                " UNION " +
                " select h.TransactionNumber as transaction_number, t.RequestType as request_type, " +
                " 'Shipped' as transaction_status, min(h.DateTime) as transaction_date " +
                " from Transactions t join History h on h.TransactionNumber = t.TransactionNumber " +
                " where t.ProcessType = 'Borrowing' and t.TransactionStatus in ('Request Finished','Request Conditionalized','Cancelled by ILL Staff') and " +
                " h.UserName = 'System' and CHARINDEX('shipped', entry) > 0 and convert(varchar(11), t.TransactionDate, 112) >= '${startDate}' " +
                " group by h.TransactionNumber, t.RequestType"
    }

    def userSqlStmt = { String userTableName ->
        "select distinct substring(sys.fn_sqlvarbasetostr(hashbytes('MD5',UserName)),3,32) as user_id, Department, nvtgc " +
                "from ${userTableName} where UserName in (select UserName from Transactions)"
    }

    def orderDateSqlStmt = "update ill_tracking t set order_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Request Sent') where order_date is null"

    def shipDateSqlStmt = "update ill_tracking t set ship_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Shipped') where ship_date is null"

    def receiveDateSqlStmt = "update ill_tracking t set receive_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Awaiting Post Receipt Processing') where receive_date is null"

    def articleReceiveDateSqlStmt = "update ill_tracking t set receive_date = " +
            " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
            " transaction_status = 'Delivered to Web') where receive_date is null"

    def arrivalDateSqlStmt = "insert into ill_lending_tracking (transaction_number, request_type, arrival_date) " +
            " select transaction_number, request_type, transaction_date " +
            " from ill_lending where status = 'Awaiting Lending Request Processing'"

    def completionSqlStmt = "update ill_lending_tracking t, ill_lending l " +
            " set completion_date = transaction_date, completion_status = status " +
            " where l.transaction_number = t.transaction_number and status " +
            " not in ('Awaiting Lending Request Processing','Cancelled by ILL Staff')"

    def cancelledSqlStmt = "update ill_lending_tracking t, ill_lending l " +
            " set completion_date = transaction_date, completion_status = status " +
            " where l.transaction_number = t.transaction_number and status = 'Cancelled by ILL Staff'"
}
