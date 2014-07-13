package edu.utexas.quietplaces.test;

import edu.utexas.quietplaces.DateUtils;
import junit.framework.TestCase;
import org.joda.time.DateTime;

public class DateUtilsTest extends TestCase {

    public void testGetISO8601Date() throws Exception {

    }

    public void testParseISO8601Date() throws Exception {

    }

    public void testGetPrettyDateTime() throws Exception {
        System.err.println("Hello, test.");
        DateTime test_dtime = new DateTime(2012, 1, 1, 0, 0);
        String expected_pretty_date = "1-1-2012 00:00";
        String pretty_date = DateUtils.getPrettyDateTime(test_dtime);
        assertEquals(expected_pretty_date, pretty_date);
    }
}