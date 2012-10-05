package metridoc.illiad

import grails.test.mixin.TestFor
import groovy.sql.Sql
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

@TestFor(metridoc.illiad.IlliadWorkflowService)
class IlliadWorkflowServiceTests {

    def dataSource = new EmbeddedDatabaseBuilder(type: EmbeddedDatabaseType.H2).build()
    def sql

    @Before
    void setupService() {
        service.dataSource_illiad = dataSource
        sql = new Sql(dataSource)
        sql.execute("create table foo(bar int)")
    }

    @After
    void shutDownDataSource() {
        dataSource.shutdown()
    }

    @Test
    void "true is returned if table exists"() {
        assert service.tableExists("foo")
    }

    @Test
    void "false is returned if table does NOT exist"() {
        assert !service.tableExists("bar")
    }

    @Test
    void "if LenderAddresses exists then that is the chosen lender table"() {
        sql.execute("create table LenderAddresses")
        assert service.lenderTableName == service.LENDER_ADDRESSES
    }

    @Test
    void "if LenderAddressesAll exists then that is the chosen lender table"() {
        sql.execute("create table LenderAddressesAll")
        assert service.lenderTableName == service.LENDER_ADDRESSES_ALL
    }
}
