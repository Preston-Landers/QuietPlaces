package edu.utexas.quietplaces;

import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;

/**
 * Contains an association of a QuietPlace and a MapMarker.
 */
public class QuietPlaceMapMarker {
    private static final String TAG = Config.PACKAGE_NAME + ".QuietPlaceMapMarker";

    private QuietPlace quietPlace;
    private Marker mapMarker;
    private QPMapFragment qpMapFragment;
    private Circle circle;

    QuietPlaceMapMarker() {
    }

    public static QuietPlaceMapMarker createQuietPlaceMapMarker(
            QuietPlace quietPlace,
            QPMapFragment qpMapFragment
    ) {
        QuietPlaceMapMarker qpmm = new QuietPlaceMapMarker();
        qpmm.setQpMapFragment(qpMapFragment);
        qpmm.setQuietPlace(quietPlace);
        qpmm.setMarkerFromQP();
        return qpmm;
    }

    /**
     * Add a marker to the map for my QuietPlace
     */
    private void setMarkerFromQP() {
        final QuietPlace quietPlace = getQuietPlace();
        final String comment = quietPlace.getComment();
        GoogleMap googleMap = getQpMapFragment().getMap();

        MarkerOptions markerOptions = new MarkerOptions();
        LatLng latLng = new LatLng(quietPlace.getLatitude(), quietPlace.getLongitude());
        markerOptions.position(latLng);
        markerOptions.title(comment);

        // Draw a circle to represent the geofence
        // May need to hang on to this reference somehow for when
        // we need to adjust the radius size by gesture
        setCircle(addQuietPlaceCircle(quietPlace));


        setMapMarker(googleMap.addMarker(markerOptions));
        Log.w(TAG, "Added place to map: " + quietPlace);
    }

    /**
     * What happens when we click this map marker
     *
     * TODO: confirm before removing... maybe that should be a pref?
     *
     * @return true if this should be considered fully handled
     */
    public boolean onMarkerClick() {
        getQpMapFragment().shortToast("Marker clicked: " + getQuietPlace().getComment());
        getMapMarker().remove();
        getCircle().remove();
        getQpMapFragment().deleteQuietPlaceMapMarker(this);
        return true;
    }

    private Circle addQuietPlaceCircle(QuietPlace quietPlace) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(
                new LatLng(quietPlace.getLatitude(), quietPlace.getLongitude())
        );
        circleOptions.radius(quietPlace.getRadius());
        circleOptions.strokeColor(Config.QP_CIRCLE_STROKE_COLOR);
        circleOptions.fillColor(Config.QP_CIRCLE_FILL_COLOR);

        return getQpMapFragment().getMap().addCircle(circleOptions);
    }

    public QuietPlace getQuietPlace() {
        return quietPlace;
    }

    public void setQuietPlace(QuietPlace quietPlace) {
        this.quietPlace = quietPlace;
    }

    public Marker getMapMarker() {
        return mapMarker;
    }

    public void setMapMarker(Marker mapMarker) {
        this.mapMarker = mapMarker;
    }

    public QPMapFragment getQpMapFragment() {
        return qpMapFragment;
    }

    public void setQpMapFragment(QPMapFragment qpMapFragment) {
        this.qpMapFragment = qpMapFragment;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

}
