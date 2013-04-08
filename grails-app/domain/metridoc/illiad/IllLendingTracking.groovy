package metridoc.illiad

import metridoc.utils.DateUtil

class IllLendingTracking {

    Long transactionNumber
    String requestType
    Date arrivalDate
    Date completionDate
    String completionStatus
    Double turnaround

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        transactionNumber(unique: true)
        arrivalDate(nullable: true)
        completionDate(nullable: true)
        completionStatus(nullable: true)
    }

    static void updateTurnAroundsForAllRecords() {
        IllLendingTracking.withNewTransaction {
            IllLendingTracking.list().each {IllLendingTracking illLendingTracking ->
                illLendingTracking.turnaround = DateUtil.differenceByDays(illLendingTracking.completionDate, illLendingTracking.arrivalDate)
            }
        }
    }
}
