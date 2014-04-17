package edu.utexas.quietplaces;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Data access object for history events
 */
public class HistoryEventsContentProvider extends ContentProvider {

    private final static String TAG = Config.PACKAGE_NAME + ".HistoryEventsContentProvider";

    private HistoryEventsTable database;

    private static String[] allColumns = HistoryEventsTable.allColumns;

    // used for the UriMatcher
    private static final int EVENTS = 10;
    private static final int EVENT_ID = 20;

    private static final String AUTHORITY = Config.PACKAGE_NAME + ".events.contentprovider";
    private static final String BASE_PATH = HistoryEventsTable.TABLE_EVENTS;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/events";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/event";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, EVENTS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", EVENT_ID);
    }

    @Override
    public boolean onCreate() {
        database = new HistoryEventsTable(getContext());
        return false;
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
            values.put(HistoryEventsTable.COLUMN_ID, historyEvent.getId());
            creating = false;
        }
        values.put(HistoryEventsTable.COLUMN_TYPE, historyEvent.getType());
        values.put(HistoryEventsTable.COLUMN_TEXT, historyEvent.getText());
        values.put(HistoryEventsTable.COLUMN_DATETIME, historyEvent.getDatetimeString());
        values.put(HistoryEventsTable.COLUMN_SEEN, historyEvent.isSeen() ? 1 : 0);

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
                    HistoryEventsTable.COLUMN_ID + " LIKE ?",
                    params
            );
            Log.i(TAG, "updated history event: " + historyEvent.toString());
        }

        return loadHistoryEventById(context, objectId);
    }

    public static HistoryEvent loadHistoryEventById(Context context, Long objectId) {

        String mSelectionClause = HistoryEventsTable.COLUMN_ID + " = ?";

        // This defines a one-element String array to contain the selection argument.
        String[] mSelectionArgs = {Long.toString(objectId)};

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = context.getContentResolver().query(
                CONTENT_URI,
                allColumns,
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
     * @return
     */
    public static List<HistoryEvent> getAllHistoryEvents(Context context, boolean orderedByTime) {

        String sortOrder = orderedByTime ? HistoryEventsTable.COLUMN_DATETIME + " ASC " : null;

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = context.getContentResolver().query(
                CONTENT_URI,
                allColumns,
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
        String mSelectionClause = HistoryEventsTable.COLUMN_ID + " LIKE ?";
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
     * @param cursor active cursor
     * @return new HistoryEvent from the current record in the cursor.
     */
    protected static HistoryEvent cursorToHistoryEvent(Cursor cursor) {
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
        queryBuilder.setTables(HistoryEventsTable.TABLE_EVENTS);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case EVENTS:
                break;
            case EVENT_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(HistoryEventsTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            // TODO: add other selections here?
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase sqlDB = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(sqlDB, projection, selection,
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
                throw new IllegalArgumentException("Unknown columns in events table query");
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
        SQLiteDatabase sqlDB = getWritableDatabase();
        long id;
        switch (uriType) {
            case EVENTS:
                id = sqlDB.insert(HistoryEventsTable.TABLE_EVENTS, null, values);
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
        SQLiteDatabase sqlDB = getWritableDatabase();
        String tableName = HistoryEventsTable.TABLE_EVENTS;
        int rowsDeleted;
        switch (uriType) {
            case EVENTS:
                rowsDeleted = sqlDB.delete(tableName, selection, selectionArgs);
                break;
            case EVENT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(tableName,
                            HistoryEventsTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(tableName,
                            HistoryEventsTable.COLUMN_ID + "=" + id
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
        SQLiteDatabase sqlDB = getWritableDatabase();
        String tableName = HistoryEventsTable.TABLE_EVENTS;
        int rowsUpdated;
        switch (uriType) {
            case EVENTS:
                rowsUpdated = sqlDB.update(tableName,
                        values,
                        selection,
                        selectionArgs);
                break;
            case EVENT_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(tableName,
                            values,
                            HistoryEventsTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(tableName,
                            values,
                            HistoryEventsTable.COLUMN_ID + "=" + id
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
     * Get a writable database and throw an error if we can't
     *
     * @return writable SQLiteDatabase
     */
    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        if (sqlDB == null) {
            String msg = "Unable to get writable database";
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
        return sqlDB;
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


}
