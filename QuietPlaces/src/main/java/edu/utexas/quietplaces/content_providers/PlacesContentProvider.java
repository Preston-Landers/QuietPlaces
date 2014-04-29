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

import java.util.ArrayList;
import java.util.List;

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

    public static String[] ALL_KEYS = {
            KEY_ID,
            KEY_NAME,
            KEY_VICINITY,
            KEY_LOCATION_LAT,
            KEY_LOCATION_LNG,
            KEY_TYPES,
            KEY_VIEWPORT,
            KEY_ICON,
            KEY_REFERENCE,
            KEY_DISTANCE,
            KEY_LAST_UPDATE_TIME
    };

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.gplaces";
    private static final String BASE_PATH = PLACES_TABLE;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.quietplaces.gplaces";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.quietplaces.gplace";

    //Create the constants used to differentiate between the different URI requests.
    private static final int PLACES = 1;
    private static final int PLACE_ID = 2;

    //Allocate the UriMatcher object, where a URI ending in 'places' will
    //correspond to a request for all places, and 'places' with a trailing '/[Unique ID]' will represent a single place details row.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, PLACES_TABLE, PLACES);
        uriMatcher.addURI(AUTHORITY, PLACES_TABLE + "/*", PLACE_ID);
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

        return (placesDB != null);
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

    public static List<Place> getAllPlaces(Context context, String sortOrder) {

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = context.getContentResolver().query(
                PlacesContentProvider.CONTENT_URI,
                PlacesContentProvider.ALL_KEYS,
                null,
                null,
                sortOrder);                          // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            Log.e(TAG, "Unable to load GPlaces");
            return null;
        }

        List<Place> placeList = new ArrayList<Place>();
        mCursor.moveToFirst();
        while (mCursor.moveToNext()) {
            PlacesContentProvider.Place newPlace = PlacesContentProvider.cursorToPlace(mCursor);
            placeList.add(newPlace);
        }
        mCursor.close();  // do we need this?
        return placeList;
    }


    public static Place cursorToPlace(Cursor cursor) {
        Place place = new Place();
        place.setId(cursor.getString(0));
        place.setName(cursor.getString(1));
        place.setVicinity(cursor.getString(2));
        place.setLatitude(cursor.getFloat(3));
        place.setLongitude(cursor.getFloat(4));
        place.setTypes(cursor.getString(5));
        place.setViewport(cursor.getString(6));
        place.setIcon(cursor.getString(7));
        place.setReference(cursor.getString(8));
        place.setDistance(cursor.getFloat(9));
        place.setLastUpdateTime(cursor.getLong(10));
        return place;
    }


    // A POJO for this thingie
    public static class Place {

        private String id;
        private String name;
        private String vicinity;
        private Float latitude;
        private Float longitude;
        private String types;
        private String viewport;
        private String icon;
        private String reference;
        private Float distance;
        private Long lastUpdateTime;

        @Override
        public String toString() {
            return "GPlace ID: " + getId() + " Name: " + getName() +
                    " - vicinity: " + getVicinity() +
                    " - Lat/Lng: " + getLatitude() + "/" + getLongitude() +
                    " - types: " + getTypes() +
                    " - distance: " + getDistance() +
                    " - last up: " + getLastUpdateTime();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVicinity() {
            return vicinity;
        }

        public void setVicinity(String vicinity) {
            this.vicinity = vicinity;
        }

        public Float getLatitude() {
            return latitude;
        }

        public void setLatitude(Float latitude) {
            this.latitude = latitude;
        }

        public Float getLongitude() {
            return longitude;
        }

        public void setLongitude(Float longitude) {
            this.longitude = longitude;
        }

        public String getTypes() {
            return types;
        }

        public void setTypes(String types) {
            this.types = types;
        }

        public String getViewport() {
            return viewport;
        }

        public void setViewport(String viewport) {
            this.viewport = viewport;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public Float getDistance() {
            return distance;
        }

        public void setDistance(Float distance) {
            this.distance = distance;
        }

        public Long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(Long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String[] getTypesArray() {
            return getTypes().split("\\s+");
        }
    }
}