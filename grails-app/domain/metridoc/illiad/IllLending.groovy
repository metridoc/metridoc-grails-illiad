package metridoc.illiad

class IllLending {

    Long transactionNumber
    String requestType
    String status
    Date transactionDate

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
    }
}
