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
import edu.utexas.quietplaces.HistoryEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Data access object for history events
 */
public class HistoryEventsContentProvider extends ContentProvider {

    private final static String TAG = Config.PACKAGE_NAME + ".content_providers.HistoryEventsContentProvider";

    private SQLiteDatabase database;

    public static final String TABLE_EVENTS = "events";
    public static final String KEY_ID = "_id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TEXT = "text";
    public static final String KEY_DATETIME = "datetime";
    public static final String KEY_SEEN = "seen";

    public static String[] ALL_KEYS = {
            KEY_ID,
            KEY_TYPE,
            KEY_TEXT,
            KEY_DATETIME,
            KEY_SEEN
    };

    // used for the UriMatcher
    private static final int EVENTS = 10;
    private static final int EVENT_ID = 20;

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".provider.historyevents";
    private static final String BASE_PATH = TABLE_EVENTS;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.quietplaces.events";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.quietplaces.event";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, EVENTS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", EVENT_ID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        HistoryEventsDatabaseHelper dbHelper = new HistoryEventsDatabaseHelper(context);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            database = null;
            Log.d(TAG, "Database Opening exception");
        }

        return (database != null);
    }


    /**
     * Save a history event object to the database - either updates existing or creates new.
     * <p/>
     * A ID field of 0 (default) creates a new record, otherwise we update an existing.
     * <p/>
     * If creating a new record, this also returns a new instance
     * of the 'same' object with the id value filled in.
     *
     * @param historyEvent the event to save
     * @return new instance with updated ID field if necessary
     */
    public static HistoryEvent saveHistoryEvent(Context context, HistoryEvent historyEvent) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = context.getContentResolver();
        boolean creating = true;
        if (historyEvent.getId() != 0) {
            values.put(KEY_ID, historyEvent.getId());
            creating = false;
        }
        values.put(KEY_TYPE, historyEvent.getType());
        values.put(KEY_TEXT, historyEvent.getText());
        values.put(KEY_DATETIME, historyEvent.getDatetimeString());
        values.put(KEY_SEEN, historyEvent.isSeen() ? 1 : 0);

        long objectId;
        if (creating) {
            Uri newUri = resolver.insert(CONTENT_URI, values);
            if (newUri == null) {
                Log.e(TAG, "Failed to insert event!");
                throw new RuntimeException("Could not insert event to database.");
            }
            objectId = ContentUris.parseId(newUri);
            Log.i(TAG, "insert history event: " + historyEvent.toString());
        } else {
            objectId = historyEvent.getId();
            String[] params = new String[]{Long.toString(objectId)};
            resolver.update(
                    CONTENT_URI,
                    values,
                    KEY_ID + " LIKE ?",
                    params
            );
            Log.i(TAG, "updated history event: " + historyEvent.toString());
        }

        return loadHistoryEventById(context, objectId);
    }

    public static HistoryEvent loadHistoryEventById(Context context, Long objectId) {

        String mSelectionClause = KEY_ID + " = ?";

        // This defines a one-element String array to contain the selection argument.
        String[] mSelectionArgs = {Long.toString(objectId)};

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = context.getContentResolver().query(
                CONTENT_URI,
                ALL_KEYS,
                mSelectionClause,
                mSelectionArgs,
                null);                          // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            Log.e(TAG, "Unable to load event by ID");
            return null;
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            Log.e(TAG, "Could not find event ID " + objectId);
            return null;
        } else {
            // Insert code here to do something with the results
            mCursor.moveToFirst();
            HistoryEvent newHistoryEvent = cursorToHistoryEvent(mCursor);
            mCursor.close();
            return newHistoryEvent;
        }
    }

    /**
     * Return a list of all history events.
     *
     * @param context       activity context
     * @param orderedByTime if true, events are return in ascending order of time.
     * @return list of all history events
     */
    public static List<HistoryEvent> getAllHistoryEvents(Context context, boolean orderedByTime) {

        String sortOrder = orderedByTime ? KEY_DATETIME + " ASC " : null;

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = context.getContentResolver().query(
                CONTENT_URI,
                ALL_KEYS,
                null,
                null,
                sortOrder);                          // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            Log.e(TAG, "Unable to load events");
            return null;
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            Log.w(TAG, "Events table appears to be empty.");
            return null;
        } else {
            List<HistoryEvent> eventList = new ArrayList<HistoryEvent>();
            mCursor.moveToFirst();
            while (mCursor.moveToNext()) {
                HistoryEvent newHistoryEvent = cursorToHistoryEvent(mCursor);
                eventList.add(newHistoryEvent);
            }
            mCursor.close();
            return eventList;
        }
    }

    public static void deleteHistoryEvent(Context context, HistoryEvent event) {
        long objectId = event.getId();
        String mSelectionClause = KEY_ID + " LIKE ?";
        String[] mSelectionArgs = {Long.toString(objectId)};
        int deletedRows = context.getContentResolver().delete(
                CONTENT_URI,
                mSelectionClause,
                mSelectionArgs);
        if (deletedRows > 0) {
            Log.i(TAG, "HistoryEvent deleted " + deletedRows + " rows with id: " + objectId);
        } else {
            Log.w(TAG, "Unable to delete history event ID: " + objectId);
        }
    }

    public static int deleteAllHistoryEvents(Context context) {
        int deletedRows = context.getContentResolver().delete(
                CONTENT_URI,
                null,
                null);
        if (deletedRows > 0) {
            Log.i(TAG, "HistoryEvent deleted " + deletedRows + " rows in table wipe");
        } else {
            Log.w(TAG, "Unable to delete history event table");
        }
        return deletedRows;
    }

    /**
     * Load a HistoryEvent object out of a cursor.
     *
     * @param cursor active cursor
     * @return new HistoryEvent from the current record in the cursor.
     */
    public static HistoryEvent cursorToHistoryEvent(Cursor cursor) {
        HistoryEvent event = new HistoryEvent();
        event.setId(cursor.getLong(0));
        event.setType(cursor.getString(1));
        event.setText(cursor.getString(2));
        // TODO: probably need some exception handling here
        event.setDatetimeString(cursor.getString(3));
        event.setSeen(cursor.getInt(4) > 0);
        return event;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(TABLE_EVENTS);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case EVENTS:
                break;
            case EVENT_ID:
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
                throw new IllegalArgumentException("Unknown columns in events table query");
            }
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case EVENTS:
                return CONTENT_TYPE;
            case EVENT_ID:
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
            case EVENTS:
                id = database.insert(TABLE_EVENTS, null, values);
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
        String tableName = TABLE_EVENTS;
        int rowsDeleted;
        switch (uriType) {
            case EVENTS:
                rowsDeleted = database.delete(tableName, selection, selectionArgs);
                break;
            case EVENT_ID:
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
        String tableName = TABLE_EVENTS;
        int rowsUpdated;
        switch (uriType) {
            case EVENTS:
                rowsUpdated = database.update(tableName,
                        values,
                        selection,
                        selectionArgs);
                break;
            case EVENT_ID:
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

    /**
     * A database to store history events.
     */
    private class HistoryEventsDatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "events.db";
        private static final int DATABASE_VERSION = 1;

        // Database creation sql statement
        private static final String DATABASE_CREATE = "create table "
                + TABLE_EVENTS + "(" +
                KEY_ID + " integer primary key autoincrement, " +
                KEY_TYPE + " text not null, " +
                KEY_TEXT + " text not null, " +
                KEY_DATETIME + " text not null, " +
                KEY_SEEN + " integer not null " +
                ");";

        public HistoryEventsDatabaseHelper(Context context) {
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
                            + newVersion + ", which will destroy all old data"
            );
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
            onCreate(db);
        }

    }

}
