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

    @Test
    void "test difference by days"() {
        def now = new Date()
        def littleBitInFuture = new Date(new Date().time + DateUtil.ONE_DAY + (Long)(DateUtil.ONE_DAY / 2))
        double difference = DateUtil.differenceByDays(littleBitInFuture, now)
        assert Math.abs(difference - 1.5) < 0.001 //since we are dealing with decimals it wont be perfect
    }
}
