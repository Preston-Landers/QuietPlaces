package edu.utexas.quietplaces;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.VisibleRegion;
import org.joda.time.DateTime;

import java.util.*;

/**
 * A fragment containing the MapView plus our custom controls.
 */
public class QPMapFragment extends QPFragment {
    private static final String TAG = Config.PACKAGE_NAME + ".QPMapFragment";

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

    private boolean currentlyAddingPlace = false;

    private Set<QuietPlaceMapMarker> mapMarkerSet;
    private Map<Marker, QuietPlaceMapMarker> markerMap;

    private final boolean singleSelectMode = true;

    private ScaleGestureDetector mScaleDetector;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    protected static QPMapFragment newInstance(int sectionNumber) {
        QPMapFragment fragment = new QPMapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public QPMapFragment() {
        mapMarkerSet = new HashSet<QuietPlaceMapMarker>();
        markerMap = new HashMap<Marker, QuietPlaceMapMarker>();
    }


    public GoogleMap getMap() {
        return mMap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = savedInstanceState;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // super.onCreateView(inflater, container, savedInstanceState); // ???
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        try {
            MapsInitializer.initialize(getActivity());
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO handle this situation better
            longToast("Error: no Google Play Services!");
        }

        mMapView = (MapView) rootView.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(mBundle);
            setUpMapIfNeeded(rootView);
        }

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

        MainActivity activity = (MainActivity) getActivity();
        SettingsFragment settingsFragment = activity.getSettingsFragment();
        int mapTypeInt = settingsFragment.getMapTypeInt();
        setMapType(mapTypeInt);

        getMap().getUiSettings().setCompassEnabled(true);

        loadPlacesFromDatabase();

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
                addNewQuietPlace(latLng);
                cancelAddButton(view);
            }
        });
    }

    /**
     * Grow all selected places by a certain increment.
     * @param view unused
     */
    public void clickGrowButton(final View view) {
        Log.d(TAG, "Clicked grow button");
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.grow();
        }
    }

    /**
     * Shrink all selected places by a certain increment.
     * @param view unused
     */
    public void clickShrinkButton(final View view) {
        Log.d(TAG, "Clicked shrink button");
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.shrink();
        }
    }

    /**
     * Delete all selected places.
     * @param view unused
     */
    public void clickDeleteButton(final View view) {
        Log.d(TAG, "Clicked delete button");
        deleteMarkers(getSelectedMarkers());

        // update to non-selected mode, since we deleted selection
        setSelectionMode();
    }

    /**
     * Center on the first selected item.
     * @param view unused
     */
    public void clickCenterButton(final View view) {
        Log.d(TAG, "Clicked center button");
        for (QuietPlaceMapMarker qpMapMarker : getSelectedMarkers()) {
            qpMapMarker.centerCameraOnThis();
            break;  // only center on the first selected on in multi-select mode...
        }
    }

    public void clickEditButton(final View view) {
        Log.d(TAG, "Clicked Edit button");
        shortToast("Edit not implemented yet.");
    }


    private void changeAddButtonToClose(String newLabel) {
        ImageButton addButton = ((ImageButton) getActivity().findViewById(R.id.addPlaceButton));
        addButton.setBackgroundResource(R.drawable.ic_close_icon);

        TextView addLabel = (TextView) getActivity().findViewById(R.id.tv_addPlaceLabel);
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
        TextView addLabel = (TextView) getActivity().findViewById(R.id.tv_addPlaceLabel);
        addLabel.setText("Add"); // TODO: text

        getMap().setOnMapClickListener(null);
        currentlyAddingPlace = false;
    }

    /**
     * Adds a new QuietPlace to the map and save it to the database.
     *
     * @param latLng the lat/long coordinates of the place
     */
    private void addNewQuietPlace(LatLng latLng) {
        // shortToast("Clicked at: " + latLng);
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();

        // temporary stuff
        DateTime now = new DateTime();
        final String comment = "Created at " + DateUtils.getPrettyDateTime(now);

        // initial radius suggestion
        double radius = getSuggestedRadius();

        // Create a new object and save it to the database
        QuietPlace quietPlace = new QuietPlace();
        quietPlace.setLatitude(latLng.latitude);
        quietPlace.setLongitude(latLng.longitude);
        quietPlace.setCategory(""); // TODO
        quietPlace.setDatetime(now);
        quietPlace.setComment(comment);
        quietPlace.setRadius(radius);

        quietPlace = dataSource.saveQuietPlace(quietPlace);
        Log.w(TAG, "Saved place to db: " + quietPlace);
        dataSource.close();

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
        mapMarkerSet.remove(qpmm);
    }


    /**
     * Load the database into markers
     */
    private void loadPlacesFromDatabase() {
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();

        List<QuietPlace> quietPlaceList = dataSource.getAllQuietPlaces();

        for (QuietPlace quietPlace : quietPlaceList) {
            addQuietPlaceMapMarker(quietPlace);
        }

        dataSource.close();
        Log.w(TAG, "Loaded marker database.");
    }

    private void addQuietPlaceMapMarker(QuietPlace quietPlace) {
        QuietPlaceMapMarker qpmm = QuietPlaceMapMarker.createQuietPlaceMapMarker(quietPlace, this);
        mapMarkerSet.add(qpmm);
        markerMap.put(qpmm.getMapMarker(), qpmm);
    }

    public QuietPlaceMapMarker getQPMMFromMarker(Marker marker) {
        return markerMap.get(marker);
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
        if (numSelectedMarkers() > 0) {
            // changeAddButtonToClose("Delete"); // TODO: text
        } else {
//            final ImageButton addButton = (ImageButton) getActivity().findViewById(R.id.addPlaceButton);
//            cancelAddButton(addButton);

            showInfoBox(false, null);

        }
    }

    public void unselectAllIfSingleSelectMode() {
        if (!singleSelectMode) {
            return;
        }

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
        View infoBox = getActivity().findViewById(R.id.selected_info_container);
        View actionBox = getActivity().findViewById(R.id.selected_action_container);
        View addBox = getActivity().findViewById(R.id.add_button_container);

        if (infoBox == null || actionBox == null || addBox == null) {
            Log.e(TAG, "Can't find containers...");
            return;
        }


        if (!show) {
            infoBox.setVisibility(View.INVISIBLE);
            actionBox.setVisibility(View.INVISIBLE);
            addBox.setVisibility(View.VISIBLE);
            return;
        }

        infoBox.setVisibility(View.VISIBLE);
        actionBox.setVisibility(View.VISIBLE);
        addBox.setVisibility(View.INVISIBLE);

        if (qpMapMarker != null) {
            QuietPlace quietPlace = qpMapMarker.getQuietPlace();

            TextView infoLabel = (TextView) getActivity().findViewById(R.id.tv_selected_label);
            if (infoLabel != null) {
                infoLabel.setText(quietPlace.getComment());
            }
            updateInfoString(quietPlace);
        }
    }

    public void updateInfoString(QuietPlace quietPlace) {
        TextView sizeLabel = (TextView) getActivity().findViewById(R.id.tv_selected_size);
        String sizeStr = getString(R.string.size_label) + " " + quietPlace.getRadiusFormatted()
                // + " " + getString(R.string.position_label) + " "
                + " @ "
                + quietPlace.getLocationFormatted();
        sizeLabel.setText(sizeStr);
    }

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


    // Not sure if I'm going to use this...

/*
    public void attachScaleListener(QuietPlaceMapMarker qpMapMarker) {
        if (mScaleDetector == null) {
            Log.i(TAG, "Creating new scale detector for QP: " + qpMapMarker.getQuietPlace());
            ScaleListener listener = new ScaleListener();
            listener.setQpMapMarker(qpMapMarker);
            mScaleDetector = new ScaleGestureDetector(getActivity(), listener);
        } else {
            Log.i(TAG, "Reusing scale detector for QP: " + qpMapMarker.getQuietPlace());
        }


        View mCustomMapControls = getActivity().findViewById(R.id.customControlsContainer);
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
        View mCustomMapControls = getActivity().findViewById(R.id.customControlsContainer);
        if (mCustomMapControls != null) {
            mCustomMapControls.setOnTouchListener(null);
        }
    }
*/

}
