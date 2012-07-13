package metridoc.utils

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 7/13/12
 * Time: 10:50 AM
 */
class DateUtilTest {

    @Test
    void "testing getting fiscal year with defaults"() {
        assert 2005 == DateUtil.getFiscalYear(2004, 6)
    }
}
