package edu.utexas.quietplaces;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;

/**
 * A fragment containing the MapView plus our custom controls.
 */
public class QPMapFragment extends QPFragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

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

    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        getMap().getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onPause() {
        getMap().getUiSettings().setCompassEnabled(false);
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    public GoogleMap getMap() {
        return mMap;
    }
}
