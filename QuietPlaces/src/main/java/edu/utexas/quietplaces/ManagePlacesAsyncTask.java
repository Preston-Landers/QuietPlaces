package edu.utexas.quietplaces;

import android.os.AsyncTask;
import android.util.Log;
import edu.utexas.quietplaces.content_providers.PlacesContentProvider;
import edu.utexas.quietplaces.fragments.QPMapFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This task is launched from MainActivity when it gets the broadcast message
 * that our Google Places database has updated.
 *
 * This is where we convert these discovered places into QuietPlaces and update
 * geofences.
 */
public class ManagePlacesAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = Config.PACKAGE_NAME + ".ManagePlacesAsyncTask";

    private MainActivity mainActivity;

    private Set<String> placeTypesToMatch;

    private List<PlacesContentProvider.Place> matchedPlaces;

    private QPMapFragment qpMapFragment;
    private List<QuietPlaceMapMarker> deletables;

    ManagePlacesAsyncTask(MainActivity activity) {
        mainActivity = activity;
        placeTypesToMatch = mainActivity.getSettingsFragment().getCurrentlySelectedPlaceTypes();
        qpMapFragment = mainActivity.getMapFragment();
    }

    @Override
    protected java.lang.Void doInBackground(Void... params) {
        Log.d(TAG, "doInBackground");
        matchedPlaces = new ArrayList<PlacesContentProvider.Place>();

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
        } else {
            Log.i(TAG, "GPlaces table has " + placeList.size() + " records.");
        }

        for (PlacesContentProvider.Place place : placeList) {
            // Log.i(TAG, "Looking at place: " + place);
            if (placeMatchesCriteria(place)) {
                Log.w(TAG, "Found a place matching our criteria: " + place);
                matchedPlaces.add(place);
            }
        }

        // Need to gather deletables even if there's no matching results.
        // This cleans up old/unwanted zones
        deletables = qpMapFragment.findAutomaticPlacesToDelete(matchedPlaces);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG, "onPostExecute");

        // Delete all of the automatically added places
        if (qpMapFragment == null) {
            Log.e(TAG, "MainActivity.getMapFragment returned null.");
            return;
        }

        qpMapFragment.loadAutomaticQuietPlaces(matchedPlaces, deletables);
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
        return placeTypesToMatch.contains(placeType.trim());
    }

}
