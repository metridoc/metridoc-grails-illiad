package metridoc.illiad

class IllLenderInfo {
    String lenderCode
    String libraryName
    String billingCategory
    String address

    static mapping = {
        version(defaultValue: '0')
    }
    static constraints = {
        libraryName(nullable: true)
        billingCategory(nullable: true)
        address(maxSize: 328, nullable: true)
    }
}
