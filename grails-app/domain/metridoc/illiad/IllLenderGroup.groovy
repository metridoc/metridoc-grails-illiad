package metridoc.illiad

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 3/21/13
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */
class IllLenderGroup {
    Integer groupNo
    String lenderCode
    Integer demographic

    static mapping = {
        id(generator: "native")
        version(defaultValue: '0')
        lenderCode(index: "idx_ill_lender_group_lender_code")
    }

    static constraints = {
        demographic(nullable: true)
    }
}
