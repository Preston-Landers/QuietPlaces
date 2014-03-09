package edu.utexas.quietplaces;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * A database to store quiet place definitions.
 */
public class QuietPlacesSQLiteHelper extends SQLiteOpenHelper {

    private static final String TAG = QuietPlacesSQLiteHelper.class.getName();

    public static final String TABLE_PLACES = "places";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_COMMENT = "comment";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_DATETIME = "datetime";  // date/time added
    public static final String COLUMN_CATEGORY = "category";  // places API category, comma separated?

    private static final String DATABASE_NAME = "places.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PLACES + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_COMMENT + " text not null, " +
            COLUMN_LATITUDE + " real not null, " +
            COLUMN_LONGITUDE + " real not null, " +
            COLUMN_RADIUS + " real not null, " +
            COLUMN_DATETIME + " text not null, " +
            COLUMN_CATEGORY + " text not null " +
            ");";

    public QuietPlacesSQLiteHelper(Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
        onCreate(db);
    }

}