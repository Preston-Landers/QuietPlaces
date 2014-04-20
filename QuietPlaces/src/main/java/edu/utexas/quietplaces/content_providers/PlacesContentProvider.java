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
 * Content Provider and database for storing the list of
 * places nearby our current location
 */
public class PlacesContentProvider extends ContentProvider {

    // private final static String TAG = Config.PACKAGE_NAME + ".content_providers.PlacesContentProvider";

    /**
     * The underlying database
     */
    private SQLiteDatabase placesDB;

    private static final String TAG = "PlacesContentProvider";
    private static final String DATABASE_NAME = "gplaces.db";
    private static final int DATABASE_VERSION = 7;
    private static final String PLACES_TABLE = "gplaces";

    // TODO Replace the columns names and database creation SQL with values for your own app.
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
    public static final String KEY_LAST_UPDATE_TIME = "lastupdatetime";

    // TODO Replace this URI with something unique to your own application.
    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.gplaces";
    private static final String BASE_PATH = PLACES_TABLE;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    //Create the constants used to differentiate between the different URI requests.
    private static final int PLACES = 1;
    private static final int PLACE_ID = 2;

    //Allocate the UriMatcher object, where a URI ending in 'places' will
    //correspond to a request for all places, and 'places' with a trailing '/[Unique ID]' will represent a single place details row.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "places", PLACES);
        uriMatcher.addURI(AUTHORITY, "places/*", PLACE_ID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        PlacesDatabaseHelper dbHelper = new PlacesDatabaseHelper(context, DATABASE_NAME,
                null, DATABASE_VERSION);
        try {
            placesDB = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            placesDB = null;
            Log.d(TAG, "Database Opening exception");
        }

        return (placesDB == null) ? false : true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            // TODO: Preston fix - similar in our other content providers?
            case PLACES:
                return "vnd.android.cursor.dir/vnd.quietplaces.place";
            case PLACE_ID:
                return "vnd.android.cursor.item/vnd.quietplaces.place";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(PLACES_TABLE);

        // If this is a row query, limit the result set to the passed in row.
        switch (uriMatcher.match(uri)) {
            case PLACE_ID:
                qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
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
        Cursor c = qb.query(placesDB,
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
        long rowID = placesDB.insert(PLACES_TABLE, "nullhack", _initialValues);

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
                count = placesDB.delete(PLACES_TABLE, where, whereArgs);
                break;

            case PLACE_ID:
                String segment = uri.getPathSegments().get(1);
                count = placesDB.delete(PLACES_TABLE, KEY_ID + "="
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
                count = placesDB.update(PLACES_TABLE, values, where, whereArgs);
                break;

            case PLACE_ID:
                String segment = uri.getPathSegments().get(1);
                count = placesDB.update(PLACES_TABLE, values, KEY_ID
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
                "create table " + PLACES_TABLE + " ("
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
                        + KEY_LAST_UPDATE_TIME + " LONG); ";

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

            db.execSQL("DROP TABLE IF EXISTS " + PLACES_TABLE);
            onCreate(db);
        }
    }
}