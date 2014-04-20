package edu.utexas.quietplaces.content_providers;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import edu.utexas.quietplaces.Config;
import edu.utexas.quietplaces.QuietPlace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * ContentProvider for QuietPlace definitions
 *
 */
public class QuietPlacesContentProvider extends ContentProvider {

    private final static String TAG = Config.PACKAGE_NAME + ".content_providers.QuietPlacesContentProvider";
    private SQLiteDatabase database;

    // Database fields

    public static final String TABLE_PLACES = "places";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_COMMENT = "comment";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_DATETIME = "datetime";  // date/time added
    public static final String COLUMN_CATEGORY = "category";  // places API category, comma separated?

    private static String[] allColumns = {
            COLUMN_ID,
            COLUMN_COMMENT,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_RADIUS,
            COLUMN_DATETIME,
            COLUMN_CATEGORY
    };

    // used for the UriMatcher
    private static final int QUIETPLACES = 10;
    private static final int QUIETPLACE_ID = 20;

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.quietplaces";
    private static final String BASE_PATH = TABLE_PLACES;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/quietplaces";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/quietplace";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, QUIETPLACES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", QUIETPLACE_ID);
    }


    /**
     * Save a QuietPlace object (existing or new) to the local database.
     *
     * A ID field of 0 (default) creates a new record, otherwise we update an existing.
     *
     * If creating a new record, this also returns a new instance
     * of the 'same' object with the id value filled in.
     *
     * @param quietPlace the place to save
     * @return new instance with updated ID field if necessary
     */
    public static QuietPlace saveQuietPlace(Context context, QuietPlace quietPlace) {
        ContentValues values = new ContentValues();
        boolean creating = true;
        if (quietPlace.getId() != 0) {
            values.put(COLUMN_ID, quietPlace.getId());
            creating = false;
        }
        values.put(COLUMN_COMMENT, quietPlace.getComment());
        values.put(COLUMN_LATITUDE, quietPlace.getLatitude());
        values.put(COLUMN_LONGITUDE, quietPlace.getLongitude());
        values.put(COLUMN_RADIUS, quietPlace.getRadius());
        values.put(COLUMN_DATETIME, quietPlace.getDatetimeString());
        values.put(COLUMN_CATEGORY, quietPlace.getCategory());

        ContentResolver resolver = context.getContentResolver();

        long objectId;
        if (creating) {
            Uri result = resolver.insert(CONTENT_URI, values);
            Long objectIdLong = getObjectIdFromUri(result);
            if (objectIdLong == null) {
                Log.e(TAG, "Error: Unable to retrieve ID from QuietPlace insert!");
                return null;
            }
            objectId = objectIdLong;
        } else {
            objectId = quietPlace.getId();
            String[] params = new String[] { Long.toString(objectId) };
            resolver.update(
                    CONTENT_URI,
                    values,
                    COLUMN_ID + " = ?",
                    params
            );
        }

        Cursor cursor = resolver.query(CONTENT_URI,
                allColumns, COLUMN_ID + " = " + objectId, null,
                null);
        if (cursor == null) {
            Log.e(TAG, "Error: Unable to fetch QuietPlace from database after save!");
            return null;
        }
        cursor.moveToFirst();
        QuietPlace newQuietPlace = cursorToQuietPlace(cursor);
        cursor.close();

        Log.d(TAG, "saved quiet place to database: " + newQuietPlace.toString());

        return newQuietPlace;
    }

    private static Long getObjectIdFromUri(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null) {
            return null;
        }
        switch (sURIMatcher.match(uri)) {
            case QUIETPLACE_ID:
                String segment = pathSegments.get(1);
                return Long.parseLong(segment);
            default:
                break;
        }
        return null;
    }

    /**
     * Utility function to delete a QuietPlace object from the database.
     * @param context
     * @param place
     */
    public static void deleteQuietPlace(Context context, QuietPlace place) {
        long id = place.getId();
        Log.w(TAG, "QuietPlace deleted with id: " + id);
        context.getContentResolver().delete(CONTENT_URI,
                COLUMN_ID + " = " + id,
                null);
    }

    public static List<QuietPlace> getAllQuietPlaces(Context context) {
        List<QuietPlace> places = new ArrayList<QuietPlace>();

        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                allColumns, null, null, null);
        if (cursor == null) {
            Log.w(TAG, "null cursor from getAllQuietPlaces query");
            return null;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            QuietPlace place = cursorToQuietPlace(cursor);
            places.add(place);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return places;
    }

    private static QuietPlace cursorToQuietPlace(Cursor cursor) {
        QuietPlace place = new QuietPlace();
        place.setId(cursor.getLong(0));
        place.setComment(cursor.getString(1));
        place.setLatitude(cursor.getDouble(2));
        place.setLongitude(cursor.getDouble(3));
        place.setRadius(cursor.getDouble(4));
        // TODO: probably need some exception handling here
        place.setDatetimeString(cursor.getString(5));
        place.setCategory(cursor.getString(6));
        return place;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        QuietPlacesDatabaseHelper dbHelper = new QuietPlacesDatabaseHelper(context);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            database = null;
            Log.d(TAG, "Database Opening exception");
        }

        return (database != null);
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(TABLE_PLACES);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case QUIETPLACES:
                break;
            case QUIETPLACE_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            // TODO: add other selections here?
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        Cursor cursor = queryBuilder.query(database, projection, selection,
                selectionArgs, null, null, sortOrder);

        // make sure that potential listeners are getting notified
        Context context = getContext();
        if (cursor != null && context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }

        return cursor;
    }

    private void checkColumns(String[] projection) {

        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(allColumns));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in quiet places table query");
            }
        }
    }

    @Override
    public String getType(Uri uri) {
        // TODO: investigate what this should really do...
        // MIME type, which doesn't really apply here I guess
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        long id;
        switch (uriType) {
            case QUIETPLACES:
                id = database.insert(TABLE_PLACES, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        String tableName = TABLE_PLACES;
        int rowsDeleted;
        switch (uriType) {
            case QUIETPLACES:
                rowsDeleted = database.delete(tableName, selection, selectionArgs);
                break;
            case QUIETPLACE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = database.delete(tableName,
                            COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = database.delete(tableName,
                            COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        String tableName = TABLE_PLACES;
        int rowsUpdated;
        switch (uriType) {
            case QUIETPLACES:
                rowsUpdated = database.update(tableName,
                        values,
                        selection,
                        selectionArgs);
                break;
            case QUIETPLACE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = database.update(tableName,
                            values,
                            COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = database.update(tableName,
                            values,
                            COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs
                    );
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri);
        return rowsUpdated;
    }


    /**
     * Notify the content resolver of any changes to this table.
     *
     * @param uri affected URI
     */
    private void notifyChange(Uri uri) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        } else {
            Log.w(TAG, "Unable to notify change.");
        }
    }


    private class QuietPlacesDatabaseHelper extends SQLiteOpenHelper {

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

        public QuietPlacesDatabaseHelper(Context context) {
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
}
