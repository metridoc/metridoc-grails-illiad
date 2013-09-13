package metridoc.utils

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 9/13/13
 * @author Tommy Barker
 */
class DateUtilSpec extends Specification {

    void "test converting month number to month text"() {
        expect:
        a == DateUtil.getMonthName(b)

        where:
        a           | b
        "January"   | Calendar.JANUARY
        "February"  | Calendar.FEBRUARY
        "March"     | Calendar.MARCH
        "April"     | Calendar.APRIL
        "May"       | Calendar.MAY
        "June"      | Calendar.JUNE
        "July"      | Calendar.JULY
        "August"    | Calendar.AUGUST
        "September" | Calendar.SEPTEMBER
        "October"   | Calendar.OCTOBER
        "November"  | Calendar.NOVEMBER
        "December"  | Calendar.DECEMBER
    }

    void "bad month throws an error"() {
        when:
        DateUtil.getMonthName(100)

        then:
        thrown(IllegalArgumentException)
    }
}
