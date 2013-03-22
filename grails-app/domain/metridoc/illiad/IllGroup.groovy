package metridoc.illiad

class IllGroup {

    String groupName
    Integer groupNo

    static mapping = {
        version(defaultValue: '0')
    }

    static constraints = {
        groupNo(unique: true)
    }
}
