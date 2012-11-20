import groovy.sql.Sql
import metridoc.utils.DateUtil

import java.text.SimpleDateFormat

def fiscalYear = DateUtil.currentFiscalYear
def formatter = new SimpleDateFormat('yyyyMMdd')
def startDateAsDate = DateUtil.getFiscalYearStartDate(fiscalYear)
def defaultStartDate = formatter.format(startDateAsDate)

metridoc {
    illiad {

        startDate = defaultStartDate

        //TODO: really should migrate this to an illiad service and just have the config file be dedicated to sql
        truncateIlliadTablesInRepository = {Sql sql ->
            [
                    "ill_group",
                    "ill_lending",
                    "ill_borrowing",
                    "ill_user_info",
                    "ill_transaction",
                    "ill_lender_info",
                    "ill_lender_group",
                    "ill_reference_number"
            ].each {
                log.info "truncating table ${it} in the repository"
                sql.execute("truncate ${it}" as String)
            }
        }

        groupSql = "select group_name from ill_group"

        groupSqlStmt = "select distinct GroupNumber as group_no, GroupName as group_name from Groups"

        groupLinkSqlStmt = "select distinct GroupNumber as group_no, LenderString as lender_code from GroupsLink"

        lenderAddrSqlStmt = {lenderTable ->
            "select distinct LenderString as lender_code, LibraryName as library_name, " +
                    " BillingCategory as billing_category, address1+'; '+address2+'; '+address3+'; '+address4 as address " +
                    " from ${lenderTable}"
        }

        referenceNumberSqlStmt = "select distinct i.TransactionNumber as transaction_number, i.OCLCNumber as oclc, " +
                " i.Type as ref_type, i.Data as ref_number from WorldCatInformation i, Transactions t " +
                " where t.TransactionNumber = i.TransactionNumber and t.TransactionStatus in ('Request Finished','Cancelled by ILL Staff')"

        transactionSqlStmt = {startDate ->
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


        borrowingSqlStmt = {startDate ->
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

        requestDateSqlStmt = "replace into ill_tracking (transaction_number, request_type, process_type, request_date) " +
                " select transaction_number, request_type, 'Borrowing', transaction_date from ill_borrowing where " +
                " transaction_status = 'Awaiting Request Processing'"

        articleRequestDateSqlStmt = "replace into ill_tracking (transaction_number, request_type, process_type, request_date) " +
                " select transaction_number, request_type, 'Borrowing', transaction_date from ill_borrowing where " +
                " transaction_status = 'Awaiting Copyright Clearance'"

        orderDateSqlStmt = "update ill_tracking t set order_date = " +
                " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
                " transaction_status = 'Request Sent') where order_date is null"

        shipDateSqlStmt = "update ill_tracking t set ship_date = " +
                " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
                " transaction_status = 'Shipped') where ship_date is null"

        receiveDateSqlStmt = "update ill_tracking t set receive_date = " +
                " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
                " transaction_status = 'Awaiting Post Receipt Processing') where receive_date is null"

        articleReceiveDateSqlStmt = "update ill_tracking t set receive_date = " +
                " (select transaction_date from ill_borrowing l where l.transaction_number = t.transaction_number and " +
                " transaction_status = 'Delivered to Web') where receive_date is null"

        lendingSqlStmt = {startDate ->
            "select t2.TransactionNumber as transaction_number, t1.RequestType as request_type, t2.ChangedTo as status, min(t2.DateTime) as transaction_date " +
                    " from Transactions t1 join Tracking t2 on t2.TransactionNumber = t1.TransactionNumber and t1.ProcessType = 'Lending' " +
                    " where convert(varchar(11), t1.TransactionDate, 112) >= '${startDate}' and " +
                    " (t1.RequestType = 'Article' and t2.ChangedTo in ('Awaiting Lending Request Processing','Request Finished','Request Conditionalized','Cancelled by ILL Staff') or " +
                    " t1.RequestType = 'Loan' and t2.ChangedTo in ('Awaiting Lending Request Processing','Awaiting Mailing', 'Item Shipped','Request Conditionalized','Cancelled by ILL Staff')) " +
                    " group by t2.TransactionNumber, t1.RequestType, t2.ChangedTo"
        }

        arrivalDateSqlStmt = "replace into ill_lending_tracking (transaction_number, request_type, arrival_date) " +
                " select transaction_number, request_type, transaction_date " +
                " from ill_lending where status = 'Awaiting Lending Request Processing'"

        completionSqlStmt = "update ill_lending_tracking t, ill_lending l " +
                " set completion_date = transaction_date, completion_status = status " +
                " where l.transaction_number = t.transaction_number and status " +
                " not in ('Awaiting Lending Request Processing','Cancelled by ILL Staff')"

        cancelledSqlStmt = "update ill_lending_tracking t, ill_lending l " +
                " set completion_date = transaction_date, completion_status = status " +
                " where l.transaction_number = t.transaction_number and status = 'Cancelled by ILL Staff'"

        userSqlStmt = {userTable ->
            "select distinct substring(sys.fn_sqlvarbasetostr(hashbytes('MD5',UserName)),3,32) as user_id, Department, nvtgc " +
                    "from ${userTable} where UserName in (select UserName from Transactions)"
        }

        lenderCodeStmt = "select distinct lender_code from ill_lender_group"
        locAbbrevStmt = "select id, abbrev from ill_location where length(trim(abbrev))>0"
        locationStmt = "select id, upper(location) as loc from ill_location"

        //TODO: really should migrate this to an illiad service and just have the config file be dedicated to sql
        doSqlCall = {String type, String sqlStatement, Sql sql ->
            profile("update $type with sql statement $sqlStatement"){
                sql.execute(sqlStatement)
            }
        }

        //TODO: really should migrate this to an illiad service and just have the config file be dedicated to sql
        updateBorrowing = {Sql sql, config ->
            [
                    config.requestDateSqlStmt,
                    config.articleRequestDateSqlStmt,
                    config.orderDateSqlStmt,
                    config.shipDateSqlStmt,
                    config.receiveDateSqlStmt,
                    config.articleReceiveDateSqlStmt
            ].each {
                config.doSqlCall("borrowing", it, sql)
            }
        }

        //TODO: really should migrate this to an illiad service and just have the config file be dedicated to sql
        updateLending = {Sql sql, config ->

            [
                    config.arrivalDateSqlStmt,
                    config.completionSqlStmt,
                    config.cancelledSqlStmt
            ].each {
                config.doSqlCall("lending", it, sql)
            }
        }

        //TODO: really should migrate this to an illiad service and just have the config file be dedicated to sql
        updateDemographics = {Sql sql, config ->
            def states = [:]
            def locations = [:]

            def loadData = {String type, String sqlStatement, String item, Map data ->
                log.info("loading ${type} for processing using" as String)
                log.info("    ${sqlStatement}" as String)
                sql.eachRow(config.locAbbrevStmt) { data[it."${item}"] = it.id }
            }

            loadData("abbreviations", config.locAbbrevStmt, "abbrev", states)
            loadData("locations", config.locationStmt, "loc", locations)

            log.info("updating ill_lender information with demographic and location info" as String)
            sql.eachRow(config.lenderCodeStmt) {
                String code = it.lender_code
                String lenderAddrStmt = "select replace(address,';',' ') addr from ill_lender_info where address is not NULL and lender_code = '" + code + "'"
                // cycle through each address associated with lender code
                sql.eachRow(lenderAddrStmt) {
                    //println "isolating individual strings making up address..."
                    // isolate individual strings making up address
                    List address = it.addr.toString().toUpperCase().tokenize(' ')
                    //println "attempting to identify matching locations..."
                    // attempt to identify matching locations
                    address.intersect(locations.keySet()).find { x ->
                        // println "setting location " + code + " to " + x
                        sql.execute("update ill_lender_group set demographic = '" + locations.get(x) + "' where lender_code = '" + code + "'")
                    }
                    address.intersect(states.keySet()).find { x ->
                        // println "setting state " + code + " to " + x
                        sql.execute("update ill_lender_group set demographic = '" + states.get(x) + "' where lender_code = '" + code + "'")
                    }
                }
            }
        }
    }
}

metridoc.illiad.schedule = "0 0 0 * * ?"