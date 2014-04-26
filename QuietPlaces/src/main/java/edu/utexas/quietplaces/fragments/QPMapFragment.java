package edu.utexas.quietplaces.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.VisibleRegion;
import edu.utexas.quietplaces.*;
import edu.utexas.quietplaces.content_providers.PlacesContentProvider;
import edu.utexas.quietplaces.content_providers.QuietPlacesContentProvider;
import org.joda.time.DateTime;

import java.util.*;

/**
 * A fragment containing the MapView plus our custom controls.
 */
public class QPMapFragment extends BaseFragment {
    private static final String TAG = FRAG_PACKAGE + ".QPMapFragment";

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

    private boolean currentlyAddingPlace = false;

    // general set of QPMM markers
    private Set<QuietPlaceMapMarker> mapMarkerSet;

    // access our markers (QPMM) by the Google Marker
    private Map<Marker, QuietPlaceMapMarker> markerMap;

    // access our markers by the geofence ID
    private Map<String, QuietPlaceMapMarker> markerByGeofenceId;

    // private ScaleGestureDetector mScaleDetector;

    private List<Geofence> pendingGeofenceAdds;
    private List<String> pendingGeofenceIdRemoves;

    private FragmentActivity mActivity;

    private boolean needToReenableFollow = false;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static QPMapFragment newInstance(int sectionNumber) {
        QPMapFragment fragment = new QPMapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public QPMapFragment() {
        mapMarkerSet = new HashSet<QuietPlaceMapMarker>();
        markerMap = new HashMap<Marker, QuietPlaceMapMarker>();
        markerByGeofenceId = new HashMap<String, QuietPlaceMapMarker>();
        pendingGeofenceAdds = new ArrayList<Geofence>();
        pendingGeofenceIdRemoves = new ArrayList<String>();
    }


    public GoogleMap getMap() {
        return mMap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = savedInstanceState;
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        if (rootView == null) {
            Log.e(TAG, "rootView is null in onCreateView in QPMapFragment");
            return null;
        }

        MapsInitializer.initialize(getMyActivity());

        mMapView = (MapView) rootView.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(mBundle);
            setUpMapIfNeeded(rootView);
        }

        MainActivity mainActivity = (MainActivity) getActivity();

        CheckBox followingUserCB = (CheckBox) rootView.findViewById(R.id.follow_user_checkBox);
        followingUserCB.setChecked(mainActivity.isFollowingUser());

        return rootView;
    }


    private void setUpMapIfNeeded(View inflatedView) {
        if (mMap == null) {
            mMap = ((MapView) inflatedView.findViewById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        // mMap.addMarker(new MarkerOptions().position(new LatLng(30, -90)).title("Marker"));

        // Set the map type from the preference.
        MainActivity activity = (MainActivity) getMyActivity();
        setMapType(activity.getSettingsFragment().getMapTypeInt());

        getMap().getUiSettings().setCompassEnabled(true);

        loadPlacesFromDatabaseAsync();

    }

    private void setupMapListeners() {
        // Handler for when we click a marker
        getMap().setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                QuietPlaceMapMarker qpmm = getQPMMFromMarker(marker);
                if (qpmm != null) {
                    return qpmm.onMarkerClick();
                } else {
                    Log.e(TAG, "clicking map marker but can't find QPMM object");
                    return false;
                }
            }
        });

        // marker dragging / moving
        getMap().setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                Log.d(TAG, "onMarkerDragStart..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);

                QuietPlaceMapMarker qpmm = getQPMMFromMarker(arg0);
                qpmm.moveMarker(false); // don't write to db just yet.

            }

            //@SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                Log.d(TAG, "onMarkerDragEnd..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);

                QuietPlaceMapMarker qpmm = getQPMMFromMarker(arg0);
                qpmm.moveMarker(true); // save this change to the database

                animateCamera(arg0.getPosition());
            }

            @Override
            public void onMarkerDrag(Marker arg0) {
                Log.d(TAG, "onMarkerDrag...");
                QuietPlaceMapMarker qpmm = getQPMMFromMarker(arg0);
                qpmm.moveMarker(false); // don't write to db; just update circle
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
        getMap().getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
        // Compass requires extra sensors, so turn it off when not using
        // getMap().getUiSettings().setCompassEnabled(false);
    }

    @Override
    public void onDestroy() {
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public final void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    /**
     * Change the map type
     *
     * @param mapType one of the constants defined in the GoogleMap object
     */
    public void setMapType(int mapType) {
        Log.d(TAG, "Setting map type: " + mapType);
        GoogleMap map = getMap();
        if (map == null) {
            Log.e(TAG, "null map when setting type.");
            return;
        }
        map.setMapType(mapType);
    }

    /**
     * Enable the "add place" mode, where touching the map adds a new quiet place.
     *
     * @param view current view
     */
    public void clickAddButton(final View view) {
        Log.w(TAG, "Clicked the add button");

/*
        List<QuietPlaceMapMarker> selectedMarkers = getSelectedMarkers();
        if (selectedMarkers.size() > 0) {
            deleteMarkers(selectedMarkers);
            cancelAddButton(view);
            return;
        }
*/

        if (currentlyAddingPlace) {
            cancelAddButton(view);
            return;
        }

        changeAddButtonToClose("Cancel");
        currentlyAddingPlace = true;

        getMap().setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                addNewQuietPlace(generateManualQuietPlace(latLng));
                cancelAddButton(view);
            }
        });
    }

    /**
     * Grow all selected places by a certain increment.
     *
     * @param view unused
     */
    public void clickGrowButton(@SuppressWarnings("UnusedParameters") final View view) {
        Log.d(TAG, "Clicked grow button");
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.grow();
        }
    }

    /**
     * Shrink all selected places by a certain increment.
     *
     * @param view unused
     */
    public void clickShrinkButton(@SuppressWarnings("UnusedParameters") final View view) {
        Log.d(TAG, "Clicked shrink button");
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.shrink();
        }
    }

    /**
     * Delete all selected places.
     *
     * @param view unused
     */
    public void clickDeleteButton(@SuppressWarnings("UnusedParameters") final View view) {
        Log.d(TAG, "Clicked delete button");
        deleteMarkers(getSelectedMarkers());
        shortToast("Marker deleted");

        // update to non-selected mode, since we deleted selection
        setSelectionMode();
    }

    /**
     * Center on the first selected item.
     *
     * @param view unused
     */
    public void clickCenterButton(@SuppressWarnings("UnusedParameters") final View view) {
        Log.d(TAG, "Clicked center button");
        //noinspection LoopStatementThatDoesntLoop
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.centerCameraOnThis();
            break;  // only center on the first selected on in multi-select mode...
        }
    }

    public void clickEditButton(@SuppressWarnings("UnusedParameters") final View view) {
        Log.d(TAG, "Clicked Edit button");
        // shortToast("Edit not implemented yet.");

        // which selection are we dealing with?
        // multiple selection is not implemented here...
        List<QuietPlaceMapMarker> markerList = getSelectedMarkers();
        if (markerList == null || markerList.size() > 1) {
            Log.e(TAG, "Can't edit markers; zero or >1 selected");
            return;
        }
        final QuietPlaceMapMarker selectedMarker = markerList.get(0);

        final EditText input = new EditText(getMyActivity());
        input.setText(selectedMarker.getQuietPlace().getComment());
        input.setSelectAllOnFocus(true); // select exiting text on focus

        AlertDialog alert = new AlertDialog.Builder(getMyActivity())
                .setTitle(getString(R.string.edit_placename_dialog_title, selectedMarker.getQuietPlace().getId()))
                .setMessage(getString(R.string.edit_placename_dialog_msg))
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        if (value != null) {
                            selectedMarker.setComment(value.toString());

                            // update the info display with the new value
                            showInfoBox(true, selectedMarker);
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                        Log.d(TAG, "User canceled comment edit.");
                    }
                }).create();


        // does this soft keyboard still appear when a hardware kb is present?
        alert.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alert.show();
    }


    private void changeAddButtonToClose(String newLabel) {
        ImageButton addButton = ((ImageButton) getMyActivity().findViewById(R.id.addPlaceButton));
        addButton.setBackgroundResource(R.drawable.ic_close_icon);

        TextView addLabel = (TextView) getMyActivity().findViewById(R.id.tv_addPlaceLabel);
        addLabel.setText(newLabel);
    }

    /**
     * Disable the "add place" state - back to regular mode
     *
     * @param view: current view
     */
    public void cancelAddButton(View view) {
        ImageButton addButton = ((ImageButton) view.findViewById(R.id.addPlaceButton));
        addButton.setBackgroundResource(R.drawable.ic_add_icon);
        TextView addLabel = (TextView) getMyActivity().findViewById(R.id.tv_addPlaceLabel);
        addLabel.setText("Add"); // TODO: text

        getMap().setOnMapClickListener(null);
        currentlyAddingPlace = false;
    }

    /**
     * Generate a new QuietPlace object based on a manual click at a lat/lng.
     *
     * @param latLng position clicked
     * @return a new QuietPlace instance
     */
    private QuietPlace generateManualQuietPlace(LatLng latLng) {
        DateTime now = new DateTime();

        // TODO: we want to replace this with something nicer
        final String comment = "Created at " + DateUtils.getPrettyDateTime(now);

        // TODO: should be looked up from Places API
        String category = "";

        // initial radius suggestion
        double radius = getSuggestedRadius();

        QuietPlace quietPlace = new QuietPlace();
        quietPlace.setLatitude(latLng.latitude);
        quietPlace.setLongitude(latLng.longitude);
        quietPlace.setCategory(category);
        quietPlace.setDatetime(now);
        quietPlace.setComment(comment);
        quietPlace.setRadius(radius);
        quietPlace.setAutoadded(false);
        quietPlace.setGplace_id(null);
        quietPlace.setGplace_ref(null);
        return quietPlace;
    }

    private QuietPlace generateAutomaticQuietPlace(PlacesContentProvider.Place gplace) {
        DateTime now = new DateTime();

        // TODO: correct the position, which is sometimes at the corner of the property!!

        final String comment = gplace.getName();
        String category = gplace.getTypes();

        double radius = getSuggestedRadiusForPlace(gplace);

        QuietPlace quietPlace = new QuietPlace();
        quietPlace.setLatitude(gplace.getLatitude());
        quietPlace.setLongitude(gplace.getLongitude());
        quietPlace.setCategory(category);
        quietPlace.setDatetime(now);
        quietPlace.setComment(comment);
        quietPlace.setRadius(radius);
        quietPlace.setAutoadded(true);
        quietPlace.setGplace_id(gplace.getId());
        quietPlace.setGplace_ref(gplace.getReference());
        return quietPlace;
    }

    /**
     * Adds a new QuietPlace object to the map and save it to the database.
     *
     * @param quietPlace the populated QuietPlace object
     */
    private void addNewQuietPlace(QuietPlace quietPlace) {
        // shortToast("Clicked at: " + latLng);

        Log.v(TAG, "About to add new quiet place to db: " + quietPlace);
        quietPlace = QuietPlacesContentProvider.saveQuietPlace(getMyActivity(), quietPlace);
        Log.w(TAG, "Saved place to db: " + quietPlace);

        if (quietPlace == null) {
            Log.e(TAG, "Unable to save new Quiet Place to the database.");
            shortToast("Internal error: unable to save.");
            return;
        }

        // we do this outside of addQuietPlaceMapMarker because
        // we don't want these logged when loaded from the database load method.
        HistoryEvent.logEvent(
                getMyActivity(),
                HistoryEvent.TYPE_PLACE_ADD,
                quietPlace.getHistoryEventFormatted()
        );

        addQuietPlaceMapMarker(quietPlace);
    }

    /**
     * Suggest an initial radius (in meters) for the quiet place based on the
     * current zoom level of the map.
     * <p/>
     * Current process:
     * * Get the bounding box of the current map view
     * * Find the shortest dimension (horizontal or vertical)
     * * Get the distance in meters of that dimension
     * * Suggest the radius as 1/8th of that.
     *
     * @return suggested radius in meters
     */
    public double getSuggestedRadius() {
        VisibleRegion region = getMap().getProjection().getVisibleRegion();

        double horizontalDistance = getDistance(region.farLeft, region.farRight);
        double verticalDistance = getDistance(region.nearLeft, region.farLeft);
        double chosenDimension = horizontalDistance;
        if (verticalDistance < horizontalDistance) {
            chosenDimension = verticalDistance;
        }

        double radius = chosenDimension * Config.SUGGESTED_RADIUS_MULTIPLIER;
        if (radius < Config.QP_SIZE_FLOOR) {
            radius = Config.QP_SIZE_FLOOR;
        }
        return radius;
    }

    private double getSuggestedRadiusForPlace(PlacesContentProvider.Place gplace) {
        // initial radius suggestion for auto-suggested places
        // TODO: do something cooler here
        return 35;
    }

    /**
     * Get the distance in meters between two gms LatLng points.
     *
     * @param a first LatLng point
     * @param b second LatLng point
     * @return distance in meters
     */
    private static double getDistance(LatLng a, LatLng b) {
        Location aprime = new Location("");
        aprime.setLongitude(a.longitude);
        aprime.setLatitude(a.latitude);
        Location bprime = new Location("");
        bprime.setLatitude(b.latitude);
        bprime.setLongitude(b.longitude);
        return aprime.distanceTo(bprime);
    }

    public void deleteMarkers(List<QuietPlaceMapMarker> selectedMarkers) {
        if (selectedMarkers == null) {
            Log.i(TAG, "null marker list in deleteMarkers");
            return;
        }
        if (selectedMarkers.size() == 0) {
            Log.i(TAG, "empty marker list in deleteMarkers");
            return;
        }
        for (QuietPlaceMapMarker qpmm : selectedMarkers) {
            qpmm.delete();
        }
    }

    /**
     * Removes a QuietPlaceMapMarker from this map fragment.  This is supposed to be
     * called from the delete() method of the QuietPlaceMapMarker object.
     * <p/>
     * All this does is remove the object from our set collection.
     *
     * @param qpmm the map marker/place to remove from this map
     */
    public void removeQuietPlaceMapMarker(QuietPlaceMapMarker qpmm) {
        Log.w(TAG, "Removing QP from map fragment: " + qpmm.getQuietPlace());

        // TODO: catch and log exception?
        if (markerMap.remove(qpmm.getMapMarker()) == null) {
            Log.w(TAG, "couldn't remove item from markerMap: " + qpmm.toString());
        }

        if (markerByGeofenceId.remove(qpmm.getGeofenceId()) == null) {
            Log.w(TAG, "couldn't remove item from markerByGeofenceId: " + qpmm.toString());
        }

        if (!mapMarkerSet.remove(qpmm)) {
            Log.w(TAG, "couldn't remove item from mapMarkerSet: " + qpmm.toString());
        }

    }


    protected void loadPlacesFromDatabaseAsync() {
        AsyncTask<Void, Void, Void> loadDatabaseTask = new AsyncTask<Void, Void, Void>() {
            private List<QuietPlace> quietPlaceList;

            @Override
            protected Void doInBackground(Void... params) {
                quietPlaceList = QuietPlacesContentProvider.getAllQuietPlaces(getMyActivity());
                if (quietPlaceList == null) {
                    Log.e(TAG, "Unable to load quiet place marker database.");
                    return null;
                }

                if (quietPlaceList.size() == 0) {
                    Log.i(TAG, "QuietPlace database is currently empty.");
                    return null;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                deleteAutoAddedMarkers();
                activateQuietPlaces(quietPlaceList, true);
            }
        };
        loadDatabaseTask.execute();

    }

    private void deleteAutoAddedMarkers() {
        // Delete all of the auto-added markers except any that are currently selected.
        List<QuietPlaceMapMarker> deletables = new ArrayList<QuietPlaceMapMarker>();
        for (QuietPlaceMapMarker qpmm : getAutoAddedMarkers()) {
            if (qpmm.isSelected()) {
                Log.w(TAG, "Not deleting auto-QPMM because it's selected: " + qpmm);
                continue;
            }
            deletables.add(qpmm);
        }
        deleteMarkers(deletables);
    }


    public void loadAutomaticQuietPlaces(List<PlacesContentProvider.Place> matchedPlaces) {
        if (matchedPlaces == null) {
            Log.e(TAG, "null place list in loadAutomaticQuietPlaces");
            return;
        }

        Log.v(TAG, "loading " + matchedPlaces.size() + " matched automatic places.");

        // Delete all of the previously auto-added markers. Even if we don't have any new ones.
        // Except don't delete those which are still present in the new results.
        // Might be a more efficient way to do this.

        List<QuietPlaceMapMarker> deletables = new ArrayList<QuietPlaceMapMarker>();
        List<QuietPlaceMapMarker> allAutoAdded = getAutoAddedMarkers();
        for (QuietPlaceMapMarker qpmm : allAutoAdded) {
            if (qpmm.isSelected()) {
                Log.w(TAG, "Not deleting auto-QPMM because it's selected: " + qpmm);
                continue;
            }
            QuietPlace qp = qpmm.getQuietPlace();
            if (qp == null) {
                Log.e(TAG, "Not deleting auto-QPMM because its QP is null: " + qpmm);
                continue;
            }
            boolean inNewSet = false;
            for (PlacesContentProvider.Place gplace : matchedPlaces) {
                if (gplace.getId().equals(qp.getGplace_id())) {
                    inNewSet = true;
                    break;
                }
            }
            if (!inNewSet) {
                Log.i(TAG, "Deleting auto-add QP because it's not in our new results: " + qp);
                deletables.add(qpmm);
            }

        }
        if (deletables.size() > 0) {
            Log.i(TAG, "Deleting " + deletables.size() + " auto-add QPs they are not in our new results");
            deleteMarkers(deletables);
        }


        // Add all of our matched places
        List<QuietPlace> quietPlaces = new ArrayList<QuietPlace>();
        for (PlacesContentProvider.Place gplace : matchedPlaces) {
            // Check if we already have a QP with the same Google Place ID.
            QuietPlaceMapMarker existingPlace = getQPMMFromGooglePlaceId(gplace.getId());
            if (existingPlace != null) {
                Log.i(TAG, "Already have a Quiet Place with this matching Google Place ID: "  + gplace.getId() + " QPMM: " + existingPlace);
                continue;
            }

            QuietPlace quietPlace = generateAutomaticQuietPlace(gplace);

            Log.d(TAG, "Saving automatic QP to database: " + quietPlace);
            quietPlace = QuietPlacesContentProvider.saveQuietPlace(getMyActivity(), quietPlace);
            quietPlaces.add(quietPlace);
        }

        // Save/sync
        if (quietPlaces.size() > 0)
        {
            activateQuietPlaces(quietPlaces, false);
        } else {
            Log.w(TAG, "No automatic places to load.");
        }
    }

    private void activateQuietPlaces(List<QuietPlace> quietPlaceList, boolean logEvent) {
        for (QuietPlace quietPlace : quietPlaceList) {
            addQuietPlaceMapMarker(quietPlace);
        }

        Log.i(TAG, "Loaded marker database.");

        if (logEvent) {
            // Not sure we need to keep this.
            HistoryEvent.logEvent(getMyActivity(), HistoryEvent.TYPE_DATABASE_LOADED,
                    String.format("Loaded %s quiet places from the database.", quietPlaceList.size()));
        }
        syncGeofences();

        Log.d(TAG, "Setting up map listeners.");
        setupMapListeners();
    }

    private void addQuietPlaceMapMarker(QuietPlace quietPlace) {
        QuietPlaceMapMarker qpmm = QuietPlaceMapMarker.createQuietPlaceMapMarker(quietPlace, this);
        mapMarkerSet.add(qpmm);
        markerMap.put(qpmm.getMapMarker(), qpmm);
        markerByGeofenceId.put(qpmm.getGeofenceId(), qpmm);

    }

    public QuietPlaceMapMarker getQPMMFromMarker(Marker marker) {
        return markerMap.get(marker);
    }

    public QuietPlaceMapMarker getQPMMFromGeofenceId(String geofenceId) {
        return markerByGeofenceId.get(geofenceId);
    }

    /**
     * Retrieve a QuietPlaceMapMarker by it's Google Place ID. Returns null if no QPMMs match that ID.
     * @param gplaceId Google Place ID
     * @return matching map marker
     */
    public QuietPlaceMapMarker getQPMMFromGooglePlaceId(String gplaceId) {
        for (QuietPlaceMapMarker qpmm : mapMarkerSet) {
            QuietPlace qp = qpmm.getQuietPlace();
            if (qp == null) { continue; }
            String thisPlaceId = qp.getGplace_id();
            if (thisPlaceId == null) { continue; }
            if (thisPlaceId.equals(gplaceId)) {
                return qpmm;
            }
        }
        return null;
    }

    public List<QuietPlaceMapMarker> getAutoAddedMarkers() {
        List<QuietPlaceMapMarker> autoAddedMarkers = new ArrayList<QuietPlaceMapMarker>();
        for (QuietPlaceMapMarker qpmm : mapMarkerSet) {
            if (qpmm.getQuietPlace().isAutoadded()) {
                autoAddedMarkers.add(qpmm);
            }
        }
        return autoAddedMarkers;
    }


    public List<QuietPlaceMapMarker> getSelectedMarkers() {
        List<QuietPlaceMapMarker> selectedMapMarkers = new ArrayList<QuietPlaceMapMarker>();
        for (QuietPlaceMapMarker qpmm : mapMarkerSet) {
            if (qpmm.isSelected()) {
                selectedMapMarkers.add(qpmm);
            }
        }
        return selectedMapMarkers;
    }

    public int numSelectedMarkers() {
        return getSelectedMarkers().size();
    }

    public void setSelectionMode() {
        if (numSelectedMarkers() <= 0) {
            showInfoBox(false, null);
        }
    }

    public void unselectAll() {

        for (QuietPlaceMapMarker qpmm : getSelectedMarkers()) {
            if (qpmm.isSelected()) {
                qpmm.setSelected(false);
            }
        }
    }

    public void animateCamera(LatLng latLng) {
        getMap().animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public void showInfoBox(boolean show, QuietPlaceMapMarker qpMapMarker) {
        View infoBox = getMyActivity().findViewById(R.id.selected_info_container);
        View actionBox = getMyActivity().findViewById(R.id.selected_action_container);
        View addBox = getMyActivity().findViewById(R.id.add_button_container);
        View followBox = getMyActivity().findViewById(R.id.follow_container);

        if (infoBox == null || actionBox == null || addBox == null || followBox == null) {
            Log.e(TAG, "Can't find containers...");
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();

        boolean currentlyFollowing = mainActivity.isFollowingUser();

        if (!show) {
            infoBox.setVisibility(View.INVISIBLE);
            actionBox.setVisibility(View.INVISIBLE);
            followBox.setVisibility(View.VISIBLE);
            addBox.setVisibility(View.VISIBLE);
            if (needToReenableFollow) {
                mainActivity.setFollowingUser(true);
                needToReenableFollow = false;
            }
            return;
        }

        infoBox.setVisibility(View.VISIBLE);
        actionBox.setVisibility(View.VISIBLE);
        followBox.setVisibility(View.INVISIBLE);
        addBox.setVisibility(View.INVISIBLE);
        if (currentlyFollowing) {
            needToReenableFollow = true;
            mainActivity.setFollowingUser(false);
        }


        if (qpMapMarker != null) {
            QuietPlace quietPlace = qpMapMarker.getQuietPlace();

            TextView infoLabel = (TextView) getMyActivity().findViewById(R.id.tv_selected_label);
            if (infoLabel != null) {
                String infoLabelText;
                if (quietPlace.isAutoadded()) {
                    // TODO: indicate matching category?
                    infoLabelText = "Auto: " + quietPlace.getComment();
                } else {
                    infoLabelText = quietPlace.getComment();
                }
                infoLabel.setText(infoLabelText);
            }
            updateInfoString(quietPlace);
        }
    }

    public void updateInfoString(QuietPlace quietPlace) {
        TextView sizeLabel = (TextView) getMyActivity().findViewById(R.id.tv_selected_size);
        String sizeStr = getString(R.string.size_label) + " " + quietPlace.getRadiusFormatted()
                // + " " + getString(R.string.position_label) + " "
                + " @ "
                + quietPlace.getLocationFormatted();
        sizeLabel.setText(sizeStr);
    }


    // Not sure if I'm going to use this scale gesture stuff...


/*
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private QuietPlaceMapMarker qpMapMarker;

        ScaleListener() {
        }

        public QuietPlaceMapMarker getQpMapMarker() {
            return qpMapMarker;
        }

        public void setQpMapMarker(QuietPlaceMapMarker qpMapMarker) {
            this.qpMapMarker = qpMapMarker;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            QuietPlaceMapMarker qpMapMarker = getQpMapMarker();
            if (qpMapMarker == null) {
                return false;
            }
            double mScaleFactor = qpMapMarker.getScaleFactor();
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            qpMapMarker.setScaleFactor(mScaleFactor);

            // invalidate();
            return true;
        }
    }
*/

/*
    public void attachScaleListener(QuietPlaceMapMarker qpMapMarker) {
        if (mScaleDetector == null) {
            Log.i(TAG, "Creating new scale detector for QP: " + qpMapMarker.getQuietPlace());
            ScaleListener listener = new ScaleListener();
            listener.setQpMapMarker(qpMapMarker);
            mScaleDetector = new ScaleGestureDetector(getMyActivity(), listener);
        } else {
            Log.i(TAG, "Reusing scale detector for QP: " + qpMapMarker.getQuietPlace());
        }


        View mCustomMapControls = getMyActivity().findViewById(R.id.customControlsContainer);
        if (mCustomMapControls != null) {
            mCustomMapControls.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    mScaleDetector.onTouchEvent(event);
                    return true;
                }
            });
        }
    }
*/

/*
    public void detachScaleListener() {
        Log.i(TAG, "Removing scale detector.");
        mScaleDetector = null;
        View mCustomMapControls = getMyActivity().findViewById(R.id.customControlsContainer);
        if (mCustomMapControls != null) {
            mCustomMapControls.setOnTouchListener(null);
        }
    }
*/

    public void queueGeofenceAdd(Geofence geofence) {
        pendingGeofenceAdds.add(geofence);
    }

    private boolean sendGeofenceAdditions() {
        if (pendingGeofenceAdds == null ||
                pendingGeofenceAdds.size() == 0) {
            return false;
        }
        MainActivity mainActivity = (MainActivity) getMyActivity();
        if (mainActivity == null) {
            return false;
        }
        boolean rv = mainActivity.requestGeofences(pendingGeofenceAdds);

        // Create a new array; the old one is being used by the pending request.
        pendingGeofenceAdds = new ArrayList<Geofence>();
        return rv;

    }

    public void queueGeofenceIdRemove(String geofenceId) {
        pendingGeofenceIdRemoves.add(geofenceId);
    }

    private boolean sendGeofenceRemovals() {
        if (pendingGeofenceIdRemoves == null ||
                pendingGeofenceIdRemoves.size() == 0) {
            return false;
        }
        MainActivity mainActivity = (MainActivity) getMyActivity();
        if (mainActivity == null) {
            return false;
        }
        boolean rv = mainActivity.removeGeofences(pendingGeofenceIdRemoves);

        // Create a new array; the old one is being used by the pending request.
        pendingGeofenceIdRemoves = new ArrayList<String>();
        return rv;
    }

    public void syncGeofences() {
        sendGeofenceRemovals();
        if (!sendGeofenceAdditions()) {
            Log.e(TAG, "May have failed to set geofences when syncing.");
        } else {
            Log.d(TAG, "Syncing geofences: add ok.");
        }
    }

    /**
     * Notify one or more Quiet Places that they have been entered or exited
     *
     * @param geofenceIds array of geofence / QP string IDs
     * @param entered     true if we're entering this geofence, otherwise we're exiting
     */
    public void handleGeofenceTransitions(String[] geofenceIds, boolean entered) {
        for (String geofenceId : geofenceIds) {
            if (geofenceId == null) {
                continue;
            }
            QuietPlaceMapMarker qpmm = getQPMMFromGeofenceId(geofenceId);
            if (qpmm == null) {
                Log.e(TAG, "Can't find QuietPlaceMapMarker from geofence ID: " + geofenceId);
                continue;
            }
            if (entered) {
                qpmm.enterGeofence();
            } else {
                qpmm.exitGeofence();
            }
        }
    }

    /**
     * Return true if we are currently inside any geofences other than the given one.
     *
     * @param excludeQPMM exclude this one from the check (can be null)
     * @return true if we are currently inside at least 1 QuietPlaceMapMarker other than the given one.
     */
    public boolean areWeInsideOtherGeofences(QuietPlaceMapMarker excludeQPMM) {
        for (QuietPlaceMapMarker qpmm : mapMarkerSet) {
            if (qpmm.equals(excludeQPMM)) {
                continue;
            }
            if (qpmm.isCurrentlyInside()) {
                return true;
            }
        }
        return false;
    }

    public FragmentActivity getMyActivity() {
        return mActivity;
    }
}
