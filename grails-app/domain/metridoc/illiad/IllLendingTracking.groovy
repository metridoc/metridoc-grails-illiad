package metridoc.illiad

class IllLendingTracking {

    Long transactionNumber
    String requestType
    Date arrivalDate
    Date completionDate
    String completionStatus

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        transactionNumber(unique: true)
        arrivalDate(nullable: true)
        completionDate(nullable: true)
        completionStatus(nullable: true)
    }
}
