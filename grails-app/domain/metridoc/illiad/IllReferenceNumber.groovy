package metridoc.illiad

class IllReferenceNumber {

    Long transactionNumber
    String oclc
    String refType
    String refNumber

    static mapping = {
        version defaultValue: '0'
    }

    static constraints = {
        oclc(nullable: true)
        refType(nullable: true)
        refNumber(nullable: true)
    }
}
