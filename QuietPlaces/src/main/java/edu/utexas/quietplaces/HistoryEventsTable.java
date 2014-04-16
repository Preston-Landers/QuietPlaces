package edu.utexas.quietplaces;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * A database to store history events.
 */
public class HistoryEventsTable extends SQLiteOpenHelper {

    private static final String TAG = HistoryEventsTable.class.getName();

    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_SEEN = "seen";

    public static String[] allColumns = {
            COLUMN_ID,
            COLUMN_TYPE,
            COLUMN_TEXT,
            COLUMN_DATETIME,
            COLUMN_SEEN
    };

    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_EVENTS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_TYPE + " text not null, " +
            COLUMN_TEXT + " text not null, " +
            COLUMN_DATETIME + " text not null, " +
            COLUMN_SEEN + " integer not null " +
            ");";

    public HistoryEventsTable(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

}