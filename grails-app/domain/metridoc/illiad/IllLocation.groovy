package metridoc.illiad

class IllLocation {

    String location
    String abbrev

    static mapping = {
        version(defaultValue: '0')
        id(generator: "assigned")
    }

    static constraints = {
    }
}
