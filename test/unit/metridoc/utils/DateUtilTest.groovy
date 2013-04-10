package metridoc.utils

import org.junit.After
import org.junit.Test

class DateUtilTest {

    @After
    void "reset the fiscal year start month"() {
        DateUtil.FY_START_MONTH = Calendar.JULY
    }

    @Test
    void "test standard fiscal years"() {
        assert 2012 == DateUtil.getFiscalYear(2012, Calendar.JANUARY)
        assert 2012 == DateUtil.getFiscalYear(2011, Calendar.DECEMBER)
    }

    @Test
    void "testing getting fiscal year with defaults"() {
        assert 2005 == DateUtil.getFiscalYear(2004, 6)
        DateUtil.FY_START_MONTH = Calendar.JANUARY
        assert 2012 == DateUtil.getFiscalYear(2012, Calendar.JANUARY)
        assert 2012 == DateUtil.getFiscalYear(2012, Calendar.DECEMBER)
    }

    @Test
    void "test difference by days"() {
        def now = new Date()
        def littleBitInFuture = new Date(new Date().time + DateUtil.ONE_DAY + (Long) (DateUtil.ONE_DAY / 2))
        double difference = DateUtil.differenceByDays(littleBitInFuture, now)
        assert Math.abs(difference - 1.5) < 0.001 //since we are dealing with decimals it wont be perfect
    }
}
