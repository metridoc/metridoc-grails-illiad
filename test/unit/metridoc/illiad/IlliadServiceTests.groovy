package metridoc.illiad

import grails.test.mixin.TestFor
import org.junit.Test

/**
 * Created with IntelliJ IDEA on 5/1/13
 * @author Tommy Barker
 */
@TestFor(IlliadService)
class IlliadServiceTests {

    @Test
    void "test selectAllFromIllTransactionClosure"() {
        def sql = service.selectAllFromIllTransaction.call('Loan', true)
        assert "select * from ill_transaction where process_type = Borrowing and request_type= Loan" == sql.trim().replaceAll(/\n/, "").replaceAll(/\s+/, " ")
    }
}
