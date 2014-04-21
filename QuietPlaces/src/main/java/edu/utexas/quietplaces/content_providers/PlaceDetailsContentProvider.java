package edu.utexas.quietplaces.content_providers;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import edu.utexas.quietplaces.Config;

/**
 * Content Provider and database for storing the details for
 * places whose details we've either viewed or prefetched.
 */
public class PlaceDetailsContentProvider extends ContentProvider {

    private static final String TAG = Config.PACKAGE_NAME + ".content_providers.PlacesDetailsContentProvider";

    /**
     * The underlying database
     */
    private SQLiteDatabase database;
    private static final String DATABASE_NAME = "gplacedetails.db";
    private static final int DATABASE_VERSION = 4;
    private static final String PLACEDETAILS_TABLE = "gplacedetails";

    // Column Names
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_VICINITY = "vicinity";
    public static final String KEY_LOCATION_LAT = "latitude";
    public static final String KEY_LOCATION_LNG = "longitude";
    public static final String KEY_TYPES = "types";
    public static final String KEY_VIEWPORT = "viewport";
    public static final String KEY_ICON = "icon";
    public static final String KEY_REFERENCE = "reference";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_RATING = "rating";
    public static final String KEY_URL = "url";
    public static final String KEY_LAST_UPDATE_TIME = "lastupdatetime";
    public static final String KEY_FORCE_CACHE = "forcecache";

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.gplacedetails";
    private static final String BASE_PATH = PLACEDETAILS_TABLE;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.quietplaces.gplacesdetails";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.quietplaces.gplacedetails";

    //Create the constants used to differentiate between the different URI requests.
    private static final int PLACES = 1;
    private static final int PLACE_ID = 2;

    //Allocate the UriMatcher object, where a URI ending in 'places' will
    //correspond to a request for all places, and 'places' with a trailing '/[Unique ID]' will represent a single place details row.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, PLACEDETAILS_TABLE, PLACES);
        uriMatcher.addURI(AUTHORITY, PLACEDETAILS_TABLE + "/*", PLACE_ID);
    }


    @Override
    public boolean onCreate() {
        Context context = getContext();

        PlacesDatabaseHelper dbHelper = new PlacesDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            database = null;
            Log.e(TAG, "Database Opening exception");
        }

        return (database == null) ? false : true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case PLACES:
                return CONTENT_TYPE;
            case PLACE_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(PLACEDETAILS_TABLE);

        // If this is a row query, limit the result set to the passed in row.
        switch (uriMatcher.match(uri)) {
            case PLACE_ID:
                qb.appendWhere(KEY_ID + "='" + uri.getPathSegments().get(1) + "'");
                break;
            default:
                break;
        }

        // If no sort order is specified sort by date / time
        String orderBy;
        if (TextUtils.isEmpty(sort)) {
            orderBy = KEY_DISTANCE + " ASC";
        } else {
            orderBy = sort;
        }

        // Apply the query to the underlying database.
        Cursor c = qb.query(database,
                projection,
                selection, selectionArgs,
                null, null, orderBy);

        // Register the contexts ContentResolver to be notified if
        // the cursor result set changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);

        // Return a cursor to the query result.
        return c;
    }

    @Override
    public Uri insert(Uri _uri, ContentValues _initialValues) {
        // Insert the new row, will return the row number if successful.
        long rowID = database.insert(PLACEDETAILS_TABLE, "not_null", _initialValues);

        // Return a URI to the newly inserted row on success.
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }
        throw new SQLException("Failed to insert row into " + _uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;

        switch (uriMatcher.match(uri)) {
            case PLACES:
                count = database.delete(PLACEDETAILS_TABLE, where, whereArgs);
                break;

            case PLACE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.delete(PLACEDETAILS_TABLE, KEY_ID + "="
                        + segment
                        + (!TextUtils.isEmpty(where) ? " AND ("
                        + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case PLACES:
                count = database.update(PLACEDETAILS_TABLE, values, where, whereArgs);
                break;

            case PLACE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.update(PLACEDETAILS_TABLE, values, KEY_ID
                        + "=" + segment
                        + (!TextUtils.isEmpty(where) ? " AND ("
                        + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    // Helper class for opening, creating, and managing database version control
    private static class PlacesDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_CREATE =
                "create table " + PLACEDETAILS_TABLE + " ("
                        + KEY_ID + " TEXT primary key, "
                        + KEY_NAME + " TEXT, "
                        + KEY_VICINITY + " TEXT, "
                        + KEY_LOCATION_LAT + " FLOAT, "
                        + KEY_LOCATION_LNG + " FLOAT, "
                        + KEY_TYPES + " TEXT, "
                        + KEY_VIEWPORT + " TEXT, "
                        + KEY_ICON + " TEXT, "
                        + KEY_REFERENCE + " TEXT, "
                        + KEY_DISTANCE + " FLOAT, "
                        + KEY_PHONE + " TEXT, "
                        + KEY_ADDRESS + " TEXT, "
                        + KEY_RATING + " FLOAT, "
                        + KEY_URL + " TEXT, "
                        + KEY_LAST_UPDATE_TIME + " LONG, "
                        + KEY_FORCE_CACHE + " BOOLEAN); ";

        public PlacesDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + PLACEDETAILS_TABLE);
            onCreate(db);
        }
    }
}