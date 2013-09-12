package metridoc.illiad

import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.ResultSetHelperService
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.ResultSet

class IlliadReportingService {

    static final ENCODING = "utf-8"

    /**
     * doing this instead of using gorm to deal with possible memory issues
     */
    def dataSourceUnproxied
    def selectAllFromIllTransaction = { String type, boolean isBorrowing ->
        def processType = isBorrowing ? "Borrowing" : "Lending"
        """
            select *
            from ill_transaction
            where process_type = ${processType}
                and request_type= ${type}
        """
    }

    def streamIlliadDataAsCsv(String type, boolean borrowing, OutputStream outputStream) {
        def sql = new Sql(dataSourceUnproxied as DataSource)
        def resultSetHelperService = new ResultSetHelperService()
        sql.query(selectAllFromIllTransaction(type, borrowing)) { ResultSet resultSet ->
            def columns = resultSetHelperService.getColumnNames(resultSet)
            def columnsWithShortTrans = addShortTransDateColumn(columns)
            def writer = new OutputStreamWriter(outputStream, ENCODING as String)
            def csvWriter = new CSVWriter(writer)
            csvWriter.writeNext(columnsWithShortTrans)
            while(resultSet.next()) {
                def line = addShortTransDateValue(resultSet, resultSetHelperService)
                csvWriter.writeNext(line)
            }
        }
    }

    static String[] addShortTransDateColumn(String[] columnsToTransform) {
        def result = []

        result.addAll(columnsToTransform)
        result << "short_trans_date"
        return result as String[]
    }

    static String[] addShortTransDateValue(ResultSet resultSet, ResultSetHelperService resultSetHelperService) {
        def values = resultSetHelperService.getColumnValues(resultSet) as List
        def transactionDate = resultSet.getDate("transaction_date")
        values << transactionDate.format("yyyy-MM-dd")

        return values as String[]
    }
}
