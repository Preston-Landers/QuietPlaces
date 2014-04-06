package edu.utexas.quietplaces;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
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
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = Config.PACKAGE_NAME + ".QPMapFragment";

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

    private boolean currentlyAddingPlace = false;

    private Set<QuietPlaceMapMarker> mapMarkerSet;
    private Map<Marker, QuietPlaceMapMarker> markerMap;

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
        mMapView.onCreate(mBundle);
        setUpMapIfNeeded(rootView);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
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

        getMap().setMapType(GoogleMap.MAP_TYPE_HYBRID);
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
    }

    @Override
    public void onResume() {
        if (mMapView != null) {
            mMapView.onResume();
        }
        getMap().getUiSettings().setCompassEnabled(true);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mMapView != null) {
            mMapView.onPause();
        }
        // Compass requires extra sensors, so turn it off when not using
        getMap().getUiSettings().setCompassEnabled(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        super.onDestroy();
    }

    /**
     * Enable the "add place" mode, where touching the map adds a new quiet place.
     *
     * @param view current view
     */
    public void clickAddButton(final View view) {
        Log.w(TAG, "Clicked the add button");

        List<QuietPlaceMapMarker> selectedMarkers = getSelectedMarkers();
        if (selectedMarkers.size() > 0) {
            deleteMarkers(selectedMarkers);
            cancelAddButton(view);
            return;
        }

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
    private double getSuggestedRadius() {
        VisibleRegion region = getMap().getProjection().getVisibleRegion();

        double horizontalDistance = getDistance(region.farLeft, region.farRight);
        double verticalDistance = getDistance(region.nearLeft, region.farLeft);
        double chosenDimension = horizontalDistance;
        if (verticalDistance < horizontalDistance) {
            chosenDimension = verticalDistance;
        }

        return chosenDimension * Config.SUGGESTED_RADIUS_MULTIPLIER;
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
            // deleteQuietPlaceMapMarker(qpmm);
            qpmm.delete();
        }
    }

    /**
     * Delete a QuietPlaceMapMarker and the underlying object from the database.
     *
     * @param qpmm the map marker/place to delete
     */
    public void deleteQuietPlaceMapMarker(QuietPlaceMapMarker qpmm) {
        Log.w(TAG, "Deleting QP from db: " + qpmm.getQuietPlace());

        // TODO: catch and log exception?
        mapMarkerSet.remove(qpmm);
        // markerMap.remove(qpmm.getMapMarker());

        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();
        dataSource.deleteQuietPlace(qpmm.getQuietPlace());
        dataSource.close();

        // TODO: delete the geofence here?
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
            changeAddButtonToClose("Delete"); // TODO: text
        } else {
            final ImageButton addButton = (ImageButton) getActivity().findViewById(R.id.addPlaceButton);
            cancelAddButton(addButton);
        }
    }
}
