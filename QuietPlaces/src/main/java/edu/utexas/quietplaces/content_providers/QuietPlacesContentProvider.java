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
    public static final String KEY_ID = "_id";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_DATETIME = "datetime";  // date/time added
    public static final String KEY_CATEGORY = "category";  // places API category, comma separated?
    public static final String KEY_AUTOADDED = "autoadded";  // was this an automatically added place? (may be auto-removed.)
    public static final String KEY_GPLACE_ID = "gplace_id";  // Google Places API ID
    public static final String KEY_GPLACE_REF = "gplace_ref";  // Google Places API 'reference' (details key)

    private static String[] ALL_KEYS = {
            KEY_ID,
            KEY_COMMENT,
            KEY_LATITUDE,
            KEY_LONGITUDE,
            KEY_RADIUS,
            KEY_DATETIME,
            KEY_CATEGORY,
            KEY_AUTOADDED,
            KEY_GPLACE_ID,
            KEY_GPLACE_REF
    };

    // used for the UriMatcher
    private static final int QUIETPLACES = 10;
    private static final int QUIETPLACE_ID = 20;

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.quietplaces";
    private static final String BASE_PATH = TABLE_PLACES;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.quietplaces.quietplaces";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.quietplaces.quietplace";

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
            values.put(KEY_ID, quietPlace.getId());
            creating = false;
        }
        values.put(KEY_COMMENT, quietPlace.getComment());
        values.put(KEY_LATITUDE, quietPlace.getLatitude());
        values.put(KEY_LONGITUDE, quietPlace.getLongitude());
        values.put(KEY_RADIUS, quietPlace.getRadius());
        values.put(KEY_DATETIME, quietPlace.getDatetimeString());
        values.put(KEY_CATEGORY, quietPlace.getCategory());
        values.put(KEY_AUTOADDED, quietPlace.isAutoadded());
        values.put(KEY_GPLACE_ID, quietPlace.getGplace_id());
        values.put(KEY_GPLACE_REF, quietPlace.getGplace_ref());

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
                    KEY_ID + " = ?",
                    params
            );
        }

        Cursor cursor = resolver.query(CONTENT_URI,
                ALL_KEYS, KEY_ID + " = " + objectId, null,
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

    // pretty sure this isn't the best way to do this, right?
    private static Long getObjectIdFromUri(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null || pathSegments.size() < 2) {
            return null;
        }
        String segment = pathSegments.get(1);
        return Long.parseLong(segment);
    }

    /**
     * Utility function to delete a QuietPlace object from the database.
     * @param context app context
     * @param place the QuietPlace to delete
     */
    public static void deleteQuietPlace(Context context, QuietPlace place) {
        long id = place.getId();
        Log.w(TAG, "QuietPlace deleted with id: " + id);
        context.getContentResolver().delete(CONTENT_URI,
                KEY_ID + " = " + id,
                null);
    }

    public static List<QuietPlace> getAllQuietPlaces(Context context) {
        List<QuietPlace> places = new ArrayList<QuietPlace>();

        Cursor cursor = context.getContentResolver().query(CONTENT_URI,
                ALL_KEYS, null, null, null);
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

    /**
     * Instantiate a QuietPlace object from a cursor.
     * TODO: probably need some exception handling here
     * @param cursor database cursor
     * @return new QuietPlace instance from the data
     */
    public static QuietPlace cursorToQuietPlace(Cursor cursor) {
        QuietPlace place = new QuietPlace();
        place.setId(cursor.getLong(0));
        place.setComment(cursor.getString(1));
        place.setLatitude(cursor.getDouble(2));
        place.setLongitude(cursor.getDouble(3));
        place.setRadius(cursor.getDouble(4));
        place.setDatetimeString(cursor.getString(5));
        place.setCategory(cursor.getString(6));
        place.setAutoadded(cursor.getInt(7) > 0);
        place.setGplace_id(cursor.getString(8));
        place.setGplace_ref(cursor.getString(9));
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
            Log.e(TAG, "Database Opening exception", e);
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
                queryBuilder.appendWhere(KEY_ID + "="
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
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(ALL_KEYS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in quiet places table query");
            }
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case QUIETPLACES:
                return CONTENT_TYPE;
            case QUIETPLACE_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
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
                            KEY_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = database.delete(tableName,
                            KEY_ID + "=" + id
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
                            KEY_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = database.update(tableName,
                            values,
                            KEY_ID + "=" + id
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
        private static final int DATABASE_VERSION = 3;

        // Database creation sql statement
        private static final String DATABASE_CREATE = "create table "
                + TABLE_PLACES + "(" +
                KEY_ID + " integer primary key, " +
                KEY_COMMENT + " text not null, " +
                KEY_LATITUDE + " real not null, " +
                KEY_LONGITUDE + " real not null, " +
                KEY_RADIUS + " real not null, " +
                KEY_DATETIME + " text not null, " +
                KEY_CATEGORY + " text not null, " +
                KEY_AUTOADDED + " integer not null default 0, " +
                KEY_GPLACE_ID + " text, " +
                KEY_GPLACE_REF + " text " +
                ");";

        private static final String UPGRADE_V2_SQL_1 = "ALTER TABLE " + TABLE_PLACES + " ADD COLUMN " + KEY_AUTOADDED + " integer not null default 0 ";
        private static final String UPGRADE_V2_SQL_2 = "ALTER TABLE " + TABLE_PLACES + " ADD COLUMN " + KEY_GPLACE_ID + " text ";
        private static final String UPGRADE_V2_SQL_3 = "ALTER TABLE " + TABLE_PLACES + " ADD COLUMN " + KEY_GPLACE_REF + " text ";


        public QuietPlacesDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            int upgradeTo = oldVersion + 1;
            while (upgradeTo <= newVersion)
            {
                switch (upgradeTo)
                {
                    case 2:
                        Log.w(TAG, "Upgrading " + TABLE_PLACES + " database to version " + upgradeTo);
                        db.execSQL(UPGRADE_V2_SQL_1);
                        db.execSQL(UPGRADE_V2_SQL_2);
                        db.execSQL(UPGRADE_V2_SQL_3);
                        break;
                    case 3:
                        Log.w(TAG, "Upgrading " + TABLE_PLACES + " database to version " + upgradeTo);

                        // Rebuild the table w/o the autoincrement specification, which is unnecessary overhead
                        // http://sqlite.org/autoinc.html
                        db.execSQL("ALTER TABLE " + TABLE_PLACES + " RENAME TO TEMP_PLACES ");
                        db.execSQL(DATABASE_CREATE);
                        db.execSQL("INSERT INTO " + TABLE_PLACES + " SELECT * FROM TEMP_PLACES");
                        db.execSQL("DROP TABLE TEMP_PLACES");
                        break;
                    // insert new versions here...
                }
                upgradeTo++;
            }
        }

    }
}
