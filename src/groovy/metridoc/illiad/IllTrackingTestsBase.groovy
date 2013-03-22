package metridoc.illiad

import org.junit.Before

import static metridoc.illiad.IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE
import static metridoc.illiad.IllBorrowing.AWAITING_REQUEST_PROCESSING
import static metridoc.illiad.IllBorrowing.REQUEST_SENT

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 3/22/13
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class IllTrackingTestsBase {

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

        (1..15).each {
            def borrowing = new IllBorrowing()
            borrowing.transactionStatus = REQUEST_SENT
            borrowing.requestType = "Loan"
            borrowing.transactionDate = new Date() + 3 //helps distinguish between awaiting request processing
            borrowing.transactionNumber = it
            borrowing.save(flush: true)
        }
    }

    private void requestDateTesting() {
        def list = IllTracking.list()
        assert 15 == list.size()

        assert 8 == IllTracking.findAllByRequestDateLessThan(new Date() + 1).size()
        assert 7 == IllTracking.findAllByRequestDateGreaterThan(new Date() + 1).size()
    }
}
