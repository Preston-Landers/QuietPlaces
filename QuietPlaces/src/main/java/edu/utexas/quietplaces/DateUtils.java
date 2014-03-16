package edu.utexas.quietplaces;

import android.util.Log;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


public class DateUtils {

    private static final String TAG = Config.APPNAME + ".DateUtils";

    /**
     * Turn a DateTime to a YYYYMMDD style (f8) string
     * @param dateTime use null for current date
     * @return F8 style string YYYYMMDD
     */
//    public static String getF8Date(DateTime dateTime) {
//        if (dateTime == null) {
//            dateTime = new DateTime();
//        }
//        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
//        return fmt.print(dateTime);
//    }

    public static String getISO8601Date(DateTime dateTime) {
        if (dateTime == null) {
            dateTime = new DateTime();
        }
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dateTime);
    }

    public static DateTime parseISO8601Date(String datetimeString) {
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        // todo: check if this is correct
        // probably want an exception handler that returns null?
        try {
            return parser.parseDateTime(datetimeString);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse datetime str: " + datetimeString, e);
            return null;
        }
    }

    public static String getPrettyDateTime(DateTime dateTime) {
        if (dateTime == null) {
            dateTime = new DateTime();
        }
        DateTimeFormatter fmt = DateTimeFormat.forPattern("M-d-y H:m");
        return fmt.print(dateTime);
    }
}
