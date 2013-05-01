package metridoc.illiad

import au.com.bytecode.opencsv.CSVWriter
import groovy.sql.Sql
import metridoc.utils.DateUtil

import javax.sql.DataSource
import java.sql.ResultSet

class IlliadService {
    /*
        LOOK AT BOTTOM OF CLASS FOR QUERY DEFINITIONS
     */
    static final int GROUP_ID_OTHER = -2;
    static final int GROUP_ID_TOTAL = -1;
    static final String ENCODING = "utf-8"

    DataSource dataSourceUnproxied_illiad
    DataSource dataSourceUnproxied

    DataSource getIlliadDataSource() {
        if (dataSourceUnproxied_illiad) {
            return dataSourceUnproxied_illiad
        }

        return dataSourceUnproxied
    }

    def streamIlliadDataAsCsv(String type, boolean borrowing, OutputStream outputStream) {
        def sql = new Sql(dataSourceUnproxied)
        sql.query(selectAllFromIllTransaction(type, borrowing)) { ResultSet resultSet ->
            def writer = new OutputStreamWriter(outputStream, ENCODING as String)
            new CSVWriter(writer).writeAll(resultSet, true)
        }
    }

    def getBasicStatsData(fiscalYear) {
        Sql sql = new Sql(getIlliadDataSource());
        def result = ['books': [:], 'articles': [:]];
        def reportFiscalYear = fiscalYear != null ? fiscalYear : DateUtil.getCurrentFiscalYear();

        Date fiscalYearStart = DateUtil.getFiscalYearStartDate(reportFiscalYear)
        Date fiscalYearEnd = DateUtil.getFiscalYearEndDate(reportFiscalYear)
        result.books.borrowing = loadSectionData(sql, true, true, fiscalYearStart, fiscalYearEnd);
        result.books.lending = loadSectionData(sql, true, false, fiscalYearStart, fiscalYearEnd);
        result.articles.borrowing = loadSectionData(sql, false, true, fiscalYearStart, fiscalYearEnd);
        result.articles.lending = loadSectionData(sql, false, false, fiscalYearStart, fiscalYearEnd);
        return result;
    }

    def loadSectionData(sql, isBooks, isBorrowing, startDate, endDate) {

        log.info("loading sectional data with isBooks: $isBooks, isBorrowing: $isBorrowing, startDate: $startDate, endDate: $endDate")

        def pickQuery = { borrowingQuery, lendingQuery ->
            isBorrowing ? borrowingQuery : lendingQuery
        }

        def genQuery = pickQuery(transactionCountsBorrowing, transactionCountsLending)
        def genQueryAgg = pickQuery(transactionCountsBorrowingAggregate, transactionCountsLendingAggregate)
        def turnaroundQuery = pickQuery(transactionTotalTurnaroundsBorrowing, transactionTotalTurnaroundsLending)
        def turnaroundPerGroupQuery = pickQuery(transactionTurnaroundsBorrowing, transactionTurnaroundsLending)

        def requestType = isBooks ? 'Loan' : 'Article';

        def result = [:];

        def sqlParams = [requestType, startDate, endDate];
        String queryFilled = getAdjustedQuery(genQuery,
                ['add_condition': ' and transaction_status=\'Request Finished\'']);

        String queryExhausted = getAdjustedQuery(genQuery,
                ['add_condition': ' and not (transaction_status<=>\'Request Finished\')']);

        String queryFilledAgg = getAdjustedQuery(genQueryAgg,
                ['add_condition': ' and transaction_status=\'Request Finished\'']);

        String queryExhaustedAgg = getAdjustedQuery(genQueryAgg,
                ['add_condition': ' and not (transaction_status is not null and transaction_status = \'Request Finished\')']);

        profile("Running query for filledQueries (borrowing=${isBorrowing}, book=${isBooks}): " + queryFilled + " params=" + sqlParams) {
            sql.eachRow(queryFilled, sqlParams, {
                int groupId = it.getAt(0)
                def groupData = getGroupDataMap(groupId, result)
                groupData.filledRequests = it.transNum
                groupData.sumFees = it.sumFees != null ? it.sumFees : 0
            })

            def row = sql.firstRow(queryFilledAgg, sqlParams)
            def groupData = getGroupDataMap(GROUP_ID_TOTAL, result)
            groupData.filledRequests = row.transNum
            groupData.sumFees = row.sumFees != null ? row.sumFees : 0
        }

        profile("Running query for turnaroundPerGroupQuery (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundPerGroupQuery + " params=" + sqlParams) {
            sql.eachRow(turnaroundPerGroupQuery, sqlParams, {
                int groupId = it.getAt(0);
                def groupData = getGroupDataMap(groupId, result)
                setTurnarounds(isBorrowing, groupData, it)
            })
        }

        profile("Running query for turnaroundQuery (total) (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundQuery + " params=" + sqlParams) {
            def totalGroupTurnarounds = sql.firstRow(turnaroundQuery, sqlParams);
            setTurnarounds(isBorrowing, getGroupDataMap(GROUP_ID_TOTAL, result), totalGroupTurnarounds)
        }


        profile("Running query for exhausted requests (borrowing=${isBorrowing}, book=#{isBook}): " + queryExhausted + " params=" + sqlParams) {
            sql.eachRow(queryExhausted, sqlParams, {
                int groupId = it.getAt(0)
                def groupData = getGroupDataMap(groupId, result)
                groupData.exhaustedRequests = it.transNum
                groupData.sumFees += it.sumFees != null ? it.sumFees : 0
            })
            def row = sql.firstRow(queryExhaustedAgg, sqlParams)
            def groupData = getGroupDataMap(GROUP_ID_TOTAL, result)
            groupData.exhaustedRequests = row.transNum
            groupData.sumFees += row.sumFees != null ? row.sumFees : 0
        }

        log.debug(result);
        return result
    }

    private void setTurnarounds(isBorrowing, groupData, srcRow) {
        if (isBorrowing) {
            groupData.turnaroundShpRec = srcRow.turnaroundShpRec
            groupData.turnaroundReqShp = srcRow.turnaroundReqShp
            groupData.turnaroundReqRec = srcRow.turnaroundReqRec
        } else {
            groupData.turnaround = srcRow.turnaround
        }
    }

    private getGroupDataMap(groupId, container) {
        if (container.get(groupId) == null) {
            container.put(groupId, ['filledRequests': 0, 'sumFees': 0, 'exhaustedRequests': 0]);
        }
        return container.get(groupId)
    }

    private String getAdjustedQuery(query, stringMap) {
        String result = query;
        stringMap.each() { key, value ->
            result = result.replaceAll("\\{${key}\\}", value)
        };
        return result;
    }

    def getGroupList() {
        IllGroup.list()
    }

    def profile(String message, Closure closure) {
        log.info "Profiling: [${message}] START"
        def start = new Date().time
        closure.call()
        def end = new Date().time
        log.info "Profiling: [${message}] END took ${end - start} ms"
    }

    def storeCache() {
        def data = [
                basicStatsData: getBasicStatsData(null),
                groups: getGroupList()
        ]
        IllCache.update(data)
    }

    def transactionCountsBorrowing = '''
                    select lg.group_no, g.group_name,
                    count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
                        group by group_no
    		'''

    def transactionCountsBorrowingAggregate = '''
                    select count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
            '''

    def transactionTurnaroundsBorrowing = '''
                    select lg.group_no,
                    AVG(bt.turnaround_shp_rec) as turnaroundShpRec,
                    AVG(bt.turnaround_req_shp) as turnaroundReqShp,
                    AVG(bt.turnaround_req_rec) as turnaroundReqRec
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_tracking bt on t.transaction_number=bt.transaction_number
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        and request_date is not null and ship_date is not null and receive_date is not null
                        and transaction_status='Request Finished'
                        group by group_no
    		'''
    def transactionCountsLending = '''
                    select lg.group_no, g.group_name,
                    count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
                        group by group_no
    		'''

    def transactionCountsLendingAggregate = '''
                    select count(distinct t.transaction_number) transNum,
                    sum(billing_amount) as sumFees
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_group g on lg.group_no=g.group_no
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        {add_condition}
            '''

    /* Need to get turnarounds for row Total separately, to avoid double counts
(because of joining with lending_group)*/
    def transactionTurnaroundsLending = '''
                    select lg.group_no,
                    AVG(lt.turnaround) as turnaround
                    from ill_transaction t
                        left join ill_lender_group lg on t.lending_library=lg.lender_code
                        left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        and lt.completion_date is not null and lt.arrival_date is not null
                        and transaction_status='Request Finished'
                        group by group_no
    		'''

    def transactionTotalTurnaroundsBorrowing = '''
                    select AVG(bt.turnaround_shp_rec) as turnaroundShpRec,
                    AVG(bt.turnaround_req_shp) as turnaroundReqShp,
                    AVG(bt.turnaround_req_rec) as turnaroundReqRec
                    from ill_transaction t
                        left join ill_tracking bt on t.transaction_number=bt.transaction_number
                        where t.process_type='Borrowing' and t.request_type=? and transaction_date between ? and ?
                        and transaction_status='Request Finished' and request_date is not null and ship_date is not null and receive_date is not null
    		'''

    def transactionTotalTurnaroundsLending = '''
                    select AVG(lt.turnaround) as turnaround
                    from ill_transaction t
                        left join ill_lending_tracking lt on t.transaction_number=lt.transaction_number
                        where t.process_type='Lending' and t.request_type=? and transaction_date between ? and ?
                        and transaction_status='Request Finished' and lt.completion_date is not null and lt.arrival_date is not null
    		'''

    def selectAllFromIllTransaction = { String type, boolean isBorrowing ->
        def processType = isBorrowing ? "Borrowing" : "Lending"
        """
            select *
            from ill_transaction
            where process_type = ${processType}
                and request_type= ${type}
        """
    }
}
