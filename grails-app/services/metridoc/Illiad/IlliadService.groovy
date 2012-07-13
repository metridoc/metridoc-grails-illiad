package metridoc.Illiad

import javax.sql.DataSource
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import groovy.sql.Sql
import metridoc.utils.DateUtil

class IlliadService {

	private static int GROUP_ID_OTHER = -2;
	private static int GROUP_ID_TOTAL = -1;

    static transactional = true
	DataSource dataSource_illiad
    def grailsApplication

	def config = ConfigurationHolder.config

    def getBasicStatsData(fiscalYear) {
		Sql sql = new Sql(dataSource_illiad);
		def result = ['books':[:], 'articles':[:]];
		def reportFiscalYear = fiscalYear != null?fiscalYear: DateUtil.getCurrentFiscalYear();

		Date fiscalYearStart = DateUtil.getFiscalYearStartDate(reportFiscalYear)
		Date fiscalYearEnd = DateUtil.getFiscalYearEndDate(reportFiscalYear)
		result.books.borrowing = loadSectionData(sql, true, true, fiscalYearStart, fiscalYearEnd);
		result.books.lending = loadSectionData(sql, true, false, fiscalYearStart, fiscalYearEnd);
		result.articles.borrowing = loadSectionData(sql, false, true, fiscalYearStart, fiscalYearEnd);
		result.articles.lending = loadSectionData(sql, false, false, fiscalYearStart, fiscalYearEnd);
		return result;
    }

	def loadSectionData(sql, isBooks, isBorrowing, startDate, endDate){

        def queries = grailsApplication.config.metridoc.illiad.queries

        def pickQuery = {borrowingQuery, lendingQuery ->
            isBorrowing ? borrowingQuery : lendingQuery
        }

        def genQuery = pickQuery(queries.transactionCountsBorrowing, queries.transactionCountsLending)
        def turnaroundQuery = pickQuery(queries.transactionTotalTurnaroundsBorrowing, queries.transactionTotalTurnaroundsLending)
        def turnaroundPerGroupQuery = pickQuery(queries.transactionTurnaroundsBorrowing, queries.transactionTurnaroundsLending)


		def processType = isBorrowing ? 'Borrowing' : 'Lending';
		def requestType = isBooks ? 'Loan' : 'Article';

		def result = [:];

		def sqlParams = [requestType, startDate, endDate];
		String queryFilled = getAdjustedQuery(genQuery,
			['add_condition': ' and transaction_status=\'Request Finished\'']);

		String queryExhausted = getAdjustedQuery(genQuery,
			['add_condition': ' and not (transaction_status<=>\'Request Finished\')']);

		log.debug("Runnig query for filledQueries (borrowing=${isBorrowing}, book=#{isBook}): " + queryFilled + " params="+sqlParams)
		sql.eachRow(queryFilled, sqlParams, {
			int groupId = it.getAt(0) != null?it.getAt(0):GROUP_ID_TOTAL;
			def groupData = getGroupDataMap(groupId, result)
			groupData.filledRequests = it.transNum
			groupData.sumFees = it.sumFees
			//setTurnarounds(isBorrowing, groupData, it)
		})

		log.debug("Runnig query for turnaroundPerGroupQuery (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundPerGroupQuery + " params="+sqlParams)
		sql.eachRow(turnaroundPerGroupQuery, sqlParams, {
			int groupId = it.getAt(0);
			def groupData = getGroupDataMap(groupId, result)
			setTurnarounds(isBorrowing, groupData, it)
		})

		/* Set corrected turnarounds for total row(rollup double counts
		   because transaction can belong to more then one group) */
		log.debug("Runnig query for turnaroundQuery (total) (borrowing=${isBorrowing}, book=#{isBook}): " + turnaroundQuery + " params="+sqlParams)
		def totalGroupTurnarounds = sql.firstRow(turnaroundQuery, sqlParams);
		setTurnarounds(isBorrowing, getGroupDataMap(GROUP_ID_TOTAL, result), totalGroupTurnarounds)


		log.debug("Runnig query for exhausted requests (borrowing=${isBorrowing}, book=#{isBook}): " + queryExhausted + " params="+sqlParams)
		sql.eachRow(queryExhausted, sqlParams, {
			int groupId = it.getAt(0) != null?it.getAt(0):GROUP_ID_TOTAL;
			def groupData = getGroupDataMap(groupId, result)
			groupData.exhaustedRequests = it.transNum
			groupData.sumFees += it.sumFees
		})

		log.debug(result);
		return result
	}
	private void setTurnarounds(isBorrowing, groupData, srcRow){
		if(isBorrowing){
			groupData.turnaroundShpRec=srcRow.turnaroundShpRec
			groupData.turnaroundReqShp=srcRow.turnaroundReqShp
			groupData.turnaroundReqRec=srcRow.turnaroundReqRec
		}else{
			groupData.turnaround = srcRow.turnaround
		}
	}
	private int getAdjustedGroupId(groupId, container){
		if(groupId == GROUP_ID_TOTAL && ! container.contains(GROUP_ID_OTHER)){
			return GROUP_ID_OTHER
		}else{
			return groupId;
		}
	}
	private getGroupDataMap(groupId, container){
		if(container.get(groupId) == null){
			container.put(groupId, ['filledRequests':0, 'sumFees':0, 'exhaustedRequests':0]);
		}
		return container.get(groupId)
	}

	private String getAdjustedQuery(query, stringMap){
		String result = query;
		stringMap.each() { key, value ->
			result = result.replaceAll("\\{${key}\\}", value)
		};
		return result;
	}

	def getGroupList(){
		Sql sql = new Sql(dataSource_illiad);
		return sql.rows(config.queries.illiad.lenderGroupList, [])
	}
}
