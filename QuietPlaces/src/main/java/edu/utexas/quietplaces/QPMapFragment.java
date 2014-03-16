package edu.utexas.quietplaces;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.joda.time.DateTime;

import java.util.Iterator;
import java.util.List;

/**
 * A fragment containing the MapView plus our custom controls.
 */
public class QPMapFragment extends QPFragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "QPMapFragment";

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

    private boolean currentlyAddingPlace = false;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        getMap().getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onPause() {
        // Compass requires extra sensors, so turn it off when not using
        getMap().getUiSettings().setCompassEnabled(false);
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    /**
     * Enable the "add place" mode, where touching the map adds a new quiet place.
     *
     * @param view current view
     */
    public void clickAddButton(final View view) {
        Log.w(TAG, "Clicked the add button");

        if (currentlyAddingPlace) {
            cancelAddButton(view);
            return;
        }

        ImageButton addButton = ((ImageButton) view.findViewById(R.id.addPlaceButton));
        addButton.setBackgroundResource(R.drawable.ic_close_icon);

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
     * Disable the "add place" state - back to regular mode
     *
     * @param view: current view
     */
    public void cancelAddButton(View view) {
        ImageButton addButton = ((ImageButton) view.findViewById(R.id.addPlaceButton));
        addButton.setBackgroundResource(R.drawable.ic_add_icon);
        getMap().setOnMapClickListener(null);
        currentlyAddingPlace = false;
    }

    /**
     * Adds a new QuietPlace to the map and save it to the database.
     * @param latLng the lat/long coordinates of the place
     */
    public void addNewQuietPlace(LatLng latLng) {
        // shortToast("Clicked at: " + latLng);
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();

        // temporary stuff
        DateTime now = new DateTime();
        final String comment = "Created at " + DateUtils.getPrettyDateTime(now);

        // Radius??
        double radius = 2.0; // TODO

        // Save it to the database

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

        setMarkerFromQP(quietPlace);
    }

    /**
     * Delete a QuietPlace object from the database.
     * @param quietPlace
     */
    public void deleteQuietPlace(QuietPlace quietPlace) {
        Log.w(TAG, "Deleting QP from db: " + quietPlace);
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();
        dataSource.deleteQuietPlace(quietPlace);
        dataSource.close();
    }

    /**
     * Add a marker to the map for a given QuietPlace
     *
     * @param quietPlace the Place to add to the map
     */
    private void setMarkerFromQP(final QuietPlace quietPlace) {
        final String comment = quietPlace.getComment();
        GoogleMap googleMap = getMap();

        MarkerOptions markerOptions = new MarkerOptions();
        LatLng latLng = new LatLng(quietPlace.getLatitude(), quietPlace.getLongitude());
        markerOptions.position(latLng);
        markerOptions.title(comment);

        // TODO: confirm before removing... maybe that should be a pref?
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                shortToast("Marker clicked: " + comment);
                marker.remove();
                deleteQuietPlace(quietPlace);
                return true;
            }
        });

        googleMap.addMarker(markerOptions);
        Log.w(TAG, "Added place to map: " + quietPlace);
    }

    /**
     * Load the database into markers
     */
    public void loadPlacesFromDatabase() {
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getActivity());
        dataSource.open();

        List<QuietPlace> quietPlaceList = dataSource.getAllQuietPlaces();

        for (Iterator<QuietPlace> it = quietPlaceList.iterator(); it.hasNext(); ) {
            QuietPlace quietPlace = it.next();
            setMarkerFromQP(quietPlace);
        }

        dataSource.close();
        Log.w(TAG, "Loaded marker database.");
    }

}
