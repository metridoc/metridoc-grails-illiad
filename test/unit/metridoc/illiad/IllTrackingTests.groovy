package metridoc.illiad

import grails.test.mixin.Mock
import metridoc.utils.DateUtil
import org.junit.Test

import static metridoc.illiad.IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE
import static metridoc.illiad.IllBorrowing.AWAITING_REQUEST_PROCESSING

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 3/22/13
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
@Mock([IllBorrowing, IllTracking])
class IllTrackingTests {

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
        addAwaitingRequestProcessingToBorrowing()
        IllTracking.updateFromIllBorrowing_AwaitingCopyrightClearance()
        IllTracking.updateFromIllBorrowing_AwaitingRequestProcessing()

        requestDateTesting()

    }

    @Test
    void "test updating IllTransaction with Awaiting CopyrightClearance"() {
        addAwaitingRequestProcessingToBorrowing()
        IllTracking.updateFromIllBorrowing_AwaitingCopyrightClearance()

        def list = IllTracking.list()
        assert 7 == list.size()
        assert 7 == IllTracking.findAllByRequestDateGreaterThan(new Date() + 1).size()
    }

    @Test
    void "test updating turnarounds"() {
        def illTracking = new IllTracking()
        illTracking.requestDate = new Date() //now

        illTracking.receiveDate = new Date(new Date().time + DateUtil.ONE_DAY + (Long)(DateUtil.ONE_DAY / 2))
        IllTracking.updateTurnArounds(illTracking)
        assert Math.abs(illTracking.turnaround_req_rec - 1.5) < 0.001 //since we are dealing with decimals it wont be perfect
    }



    private void requestDateTesting() {
        def list = IllTracking.list()
        assert 15 == list.size()

        assert 8 == IllTracking.findAllByRequestDateLessThan(new Date() + 1).size()
        assert 7 == IllTracking.findAllByRequestDateGreaterThan(new Date() + 1).size()
    }
}
