package edu.utexas.quietplaces;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Data access object for QuietPlaces
 *
 * TODO: need 'read one by id' and 'update' methods ?
 */
public class QuietPlacesDataSource {

    private final static String TAG = Config.PACKAGE_NAME + ".QuietPlacesDataSource";

    // Database fields
    private SQLiteDatabase database;
    private QuietPlacesSQLiteHelper dbHelper;
    private String[] allColumns = {
            QuietPlacesSQLiteHelper.COLUMN_ID,
            QuietPlacesSQLiteHelper.COLUMN_COMMENT,
            QuietPlacesSQLiteHelper.COLUMN_LATITUDE,
            QuietPlacesSQLiteHelper.COLUMN_LONGITUDE,
            QuietPlacesSQLiteHelper.COLUMN_RADIUS,
            QuietPlacesSQLiteHelper.COLUMN_DATETIME,
            QuietPlacesSQLiteHelper.COLUMN_CATEGORY
    };

    public QuietPlacesDataSource(Context context) {
        dbHelper = new QuietPlacesSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
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
    public QuietPlace saveQuietPlace(QuietPlace quietPlace) {
        ContentValues values = new ContentValues();
        boolean creating = true;
        if (quietPlace.getId() != 0) {
            values.put(QuietPlacesSQLiteHelper.COLUMN_ID, quietPlace.getId());
            creating = false;
        }
        values.put(QuietPlacesSQLiteHelper.COLUMN_COMMENT, quietPlace.getComment());
        values.put(QuietPlacesSQLiteHelper.COLUMN_LATITUDE, quietPlace.getLatitude());
        values.put(QuietPlacesSQLiteHelper.COLUMN_LONGITUDE, quietPlace.getLongitude());
        values.put(QuietPlacesSQLiteHelper.COLUMN_RADIUS, quietPlace.getRadius());
        values.put(QuietPlacesSQLiteHelper.COLUMN_DATETIME, quietPlace.getDatetimeString());
        values.put(QuietPlacesSQLiteHelper.COLUMN_CATEGORY, quietPlace.getCategory());

        long objectId;
        if (creating) {
            objectId = database.insert(QuietPlacesSQLiteHelper.TABLE_PLACES, null,
                    values);
        } else {
            objectId = quietPlace.getId();
            String[] params = new String[] { Long.toString(objectId) };
            database.update(
                    QuietPlacesSQLiteHelper.TABLE_PLACES,
                    values,
                    QuietPlacesSQLiteHelper.COLUMN_ID + " = ?",
                    params
            );
        }

        Cursor cursor = database.query(QuietPlacesSQLiteHelper.TABLE_PLACES,
                allColumns, QuietPlacesSQLiteHelper.COLUMN_ID + " = " + objectId, null,
                null, null, null);
        cursor.moveToFirst();
        QuietPlace newQuietPlace = cursorToQuietPlace(cursor);
        cursor.close();

        Log.d(TAG, "saved quiet place to database: " + newQuietPlace.toString());

        return newQuietPlace;
    }

    public void deleteQuietPlace(QuietPlace place) {
        long id = place.getId();
        Log.w(TAG, "QuietPlace deleted with id: " + id);
        database.delete(QuietPlacesSQLiteHelper.TABLE_PLACES,
                QuietPlacesSQLiteHelper.COLUMN_ID + " = " + id,
                null);
    }

    public List<QuietPlace> getAllQuietPlaces() {
        List<QuietPlace> places = new ArrayList<QuietPlace>();

        Cursor cursor = database.query(QuietPlacesSQLiteHelper.TABLE_PLACES,
                allColumns, null, null, null, null, null);

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

    private QuietPlace cursorToQuietPlace(Cursor cursor) {
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
}
