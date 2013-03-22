package metridoc.illiad

import grails.test.mixin.Mock
import org.junit.Before
import org.junit.Test

import static metridoc.illiad.IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE
import static metridoc.illiad.IllBorrowing.AWAITING_REQUEST_PROCESSING
import static metridoc.illiad.IllBorrowing.REQUEST_SENT

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 3/22/13
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
@Mock([IllBorrowing, IllTracking])
class IllTrackingTests {

    @Before
    void addAwaitingRequestProcessingToBorrowing() {
        (1..10).each {
            def borrowing = new IllBorrowing()
            borrowing.transactionStatus = AWAITING_REQUEST_PROCESSING
            borrowing.requestType = "Loan"
            borrowing.transactionDate = new Date()
            borrowing.transactionNumber = it
            borrowing.save(flush: true)
        }

        (9..15).each {
            def borrowing = new IllBorrowing()
            borrowing.transactionStatus = AWAITING_COPYRIGHT_CLEARANCE
            borrowing.requestType = "Loan"
            borrowing.transactionDate = new Date() + 2 //helps distinguish between awaiting request processing
            borrowing.transactionNumber = it
            borrowing.save(flush: true)
        }
    }

    @Test
    void "test updating IllTransaction with Awaiting Request Processing"() {
        IllTracking.updateFromIllBorrowing_AwaitingCopyrightClearance()
        IllTracking.updateFromIllBorrowing_AwaitingRequestProcessing()

        requestDateTesting()

    }

    @Test
    void "test updating IllTransaction with Awaiting CopyrightClearance"() {
        IllTracking.updateFromIllBorrowing_AwaitingCopyrightClearance()

        def list = IllTracking.list()
        assert 7 == list.size()
        assert 7 == IllTracking.findAllByRequestDateGreaterThan(new Date() + 1).size()
    }

    private void requestDateTesting() {
        def list = IllTracking.list()
        assert 15 == list.size()

        assert 8 == IllTracking.findAllByRequestDateLessThan(new Date() + 1).size()
        assert 7 == IllTracking.findAllByRequestDateGreaterThan(new Date() + 1).size()
    }
}
