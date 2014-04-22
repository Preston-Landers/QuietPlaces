package edu.utexas.quietplaces;

import android.os.AsyncTask;
import android.util.Log;
import edu.utexas.quietplaces.content_providers.PlacesContentProvider;

import java.util.List;
import java.util.Set;

/**
 * A periodically executing task that scans the current nearby Google Places
 * and converts them into QuietPlaces as necessary
 */
public class ManagePlacesAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = Config.PACKAGE_NAME + ".ManagePlacesAsyncTask";

    private MainActivity mainActivity;

    private Set<String> tempPlacesSet;

    ManagePlacesAsyncTask(MainActivity activity) {
        mainActivity = activity;
        tempPlacesSet = mainActivity.getPlacesAPITypesOfInterestSet();
    }

    @Override
    protected java.lang.Void doInBackground(Void... params) {
        Log.w(TAG, "doInBackground");
        if (mainActivity == null) {
            Log.e(TAG, "mainActivity is null in doInBackground, can't update places.");
            return null;
        }


        String sortOrder = null;

        List<PlacesContentProvider.Place> placeList = PlacesContentProvider.getAllPlaces(mainActivity, sortOrder);

        if (placeList == null) {
            Log.e(TAG, "GPlaces table query return null.");
            return null;
        }
        if (placeList.size() == 0) {
            Log.w(TAG, "GPlaces table appears to be empty.");
            return null;
        }

        for (PlacesContentProvider.Place place : placeList) {
            if (placeMatchesCriteria(place)) {
                Log.w(TAG, "Found a place matching our criteria: " + place);
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.w(TAG, "onPostExecute");
        // mainActivity.waitThenManagePlaces();
    }

    private boolean placeMatchesCriteria(PlacesContentProvider.Place place) {
        // Log.i(TAG, "Running criteria on place: " + place);
        for (String placeType : place.getTypesArray()) {
            if (placeTypeMatchesCriteria(placeType)) {
                Log.i(TAG, "FOUND PLACE: " + place);
                return true;
            }
        }
        return false;
    }

    private boolean placeTypeMatchesCriteria(String placeType) {
        return tempPlacesSet.contains(placeType.trim());
    }

}
