package metridoc.illiad

import groovy.sql.Sql
import metridoc.utils.DateUtil

import javax.sql.DataSource
import java.text.SimpleDateFormat

class IlliadService {

    static final int GROUP_ID_OTHER = -2;
    static final int GROUP_ID_TOTAL = -1;
    private static final FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm")

    DataSource dataSourceUnproxied_illiad
    DataSource dataSourceUnproxied

    def illiadQueriesService = new IlliadQueriesService()
    def model = Collections.synchronizedMap([:])

    DataSource getIlliadDataSource() {
        if(dataSourceUnproxied_illiad) {
            return dataSourceUnproxied_illiad
        }

        return dataSourceUnproxied
    }

    synchronized getModel() {
        synchronized (this) {
            if (model) return model
            populateModel()

            return model
        }
    }

    def populateModel() {
        synchronized (this){
            def now = new Date()
            def lastUpdate = FORMATTER.format(now)

            def data = [
                basicStatsData: getBasicStatsData(null),
                groups: getGroupList(),
                lastUpdate: lastUpdate
            ]

            model.putAll(data)
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

        def pickQuery = {borrowingQuery, lendingQuery ->
            isBorrowing ? borrowingQuery : lendingQuery
        }

        def genQuery = pickQuery(illiadQueriesService.transactionCountsBorrowing, illiadQueriesService.transactionCountsLending)
        def turnaroundQuery = pickQuery(illiadQueriesService.transactionTotalTurnaroundsBorrowing, illiadQueriesService.transactionTotalTurnaroundsLending)
        def turnaroundPerGroupQuery = pickQuery(illiadQueriesService.transactionTurnaroundsBorrowing, illiadQueriesService.transactionTurnaroundsLending)


        def processType = isBorrowing ? 'Borrowing' : 'Lending';
        def requestType = isBooks ? 'Loan' : 'Article';

        def result = [:];

        def sqlParams = [requestType, startDate, endDate];
        String queryFilled = getAdjustedQuery(genQuery,
                ['add_condition': ' and transaction_status=\'Request Finished\'']);

        String queryExhausted = getAdjustedQuery(genQuery,
                ['add_condition': ' and not (transaction_status<=>\'Request Finished\')']);

        profile("Running query for filledQueries (borrowing=${isBorrowing}, book=${isBooks}): " + queryFilled + " params=" + sqlParams) {
            sql.eachRow(queryFilled, sqlParams, {
                int groupId = it.getAt(0) != null ? it.getAt(0) : GROUP_ID_TOTAL;
                def groupData = getGroupDataMap(groupId, result)
                groupData.filledRequests = it.transNum
                groupData.sumFees = it.sumFees
//                setTurnarounds(isBorrowing, groupData, it)
            })
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
                int groupId = it.getAt(0) != null ? it.getAt(0) : GROUP_ID_TOTAL;
                def groupData = getGroupDataMap(groupId, result)
                groupData.exhaustedRequests = it.transNum
                groupData.sumFees += it.sumFees
            })
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

    private int getAdjustedGroupId(groupId, container) {
        if (groupId == GROUP_ID_TOTAL && !container.contains(GROUP_ID_OTHER)) {
            return GROUP_ID_OTHER
        } else {
            return groupId;
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
        Sql sql = new Sql(getIlliadDataSource());
        return sql.rows(illiadQueriesService.lenderGroupList, [])
    }

    def profile(String message, Closure closure) {
        log.info "Profiling: [${message}] START"
        def start = new Date().time
        closure.call()
        def end = new Date().time
        log.info "Profiling: [${message}] END took ${end - start} ms"
    }

    void updateBorrowing() {

    }
}
