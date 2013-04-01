package metridoc.illiad

import org.slf4j.LoggerFactory

import static metridoc.illiad.IllBorrowing.AWAITING_COPYRIGHT_CLEARANCE
import static metridoc.illiad.IllBorrowing.AWAITING_REQUEST_PROCESSING

class IllTracking {

    public static final String BORROWING = "Borrowing"
    Long transactionNumber
    String requestType
    String processType
    Date requestDate
    Date shipDate
    Date receiveDate
    Date orderDate


    static constraints = {
        transactionNumber(unique: true)
        requestDate(nullable: true)
        shipDate(nullable: true)
        receiveDate(nullable: true)
        orderDate(nullable: true)
    }

    static mapping = {
        version(defaultValue: '0')
        orderDate(index: "idx_ill_tracking_order_date")
        shipDate(index: "idx_ill_tracking_ship_date")
    }

    static updateFromIllBorrowing() {
        updateFromIllBorrowing_AwaitingCopyrightClearance()
        updateFromIllBorrowing_AwaitingRequestProcessing()
    }

    static updateFromIllBorrowing_AwaitingRequestProcessing() {
        Set<Long> alreadyProcessedTransactions
        //need to do a new one since this method might already be surrounded by a transaction
        IllTracking.withNewTransaction{
            alreadyProcessedTransactions = IllTracking.list().collect { it.transactionNumber } as Set
        }
        def itemsToStore = []
        LoggerFactory.getLogger(IllTracking).info "migrating all borrowing data that is awaiting request processing"
        IllBorrowing.findAllByTransactionStatus(AWAITING_REQUEST_PROCESSING).each { IllBorrowing borrowing ->
            if (!alreadyProcessedTransactions.contains(borrowing.transactionNumber)) {
                addItem(borrowing, itemsToStore)
            }
        }
        processBatch(itemsToStore)
        LoggerFactory.getLogger(IllTracking).info "finished migrating all borrowing data that is awaiting request processing"
    }

    static updateFromIllBorrowing_AwaitingCopyrightClearance() {
        LoggerFactory.getLogger(IllTracking).info "migrating all borrowing data that is awaiting copyright clearance"
        def itemsToStore = []
        IllBorrowing.findAllByTransactionStatus(AWAITING_COPYRIGHT_CLEARANCE).each { IllBorrowing borrowing ->
            addItem(borrowing, itemsToStore)
        }
        processBatch(itemsToStore)
        LoggerFactory.getLogger(IllTracking).info "finished migrating all borrowing data that is awaiting copyright clearance"
    }

    private static addItem(IllBorrowing borrowing, List<IllTracking> itemsToStore) {
        itemsToStore << createTrackingFromBorrowing(borrowing)
        if (itemsToStore.size() > 50) {
            processBatch(itemsToStore)
        }
    }

    private static IllTracking createTrackingFromBorrowing(IllBorrowing borrowing) {
        new IllTracking(
                transactionNumber: borrowing.transactionNumber,
                requestType: borrowing.requestType,
                processType: BORROWING,
                requestDate: borrowing.transactionDate
        )
    }

    private static processBatch(List<IllTracking> illTrackingList) {
        IllTracking.withNewTransaction {
            illTrackingList*.save(failOnError: true)
        }
        illTrackingList.clear()
    }
}
