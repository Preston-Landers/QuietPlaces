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
    private boolean selected;
    private double scaleFactor;

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
        markerOptions.draggable(true);

        // Draw a circle to represent the geofence
        // May need to hang on to this reference somehow for when
        // we need to adjust the radius size by gesture
        setCircle(addQuietPlaceCircle(quietPlace));

        // TODO: highly suspect...
        setScaleFactor(quietPlace.getRadius());

        setMapMarker(googleMap.addMarker(markerOptions));
        Log.w(TAG, "Added place to map: " + quietPlace);
    }

    /**
     * What happens when we click this map marker
     * <p/>
     * TODO: confirm before removing... maybe that should be a pref?
     *
     * @return true if this should be considered fully handled
     */
    public boolean onMarkerClick() {
        if (isSelected()) {
            // getQpMapFragment().shortToast("Marker un-selected: " + getQuietPlace().getComment());
            setSelected(false);
        } else {
            // getQpMapFragment().shortToast("Marker selected: " + getQuietPlace().getComment());
            setSelected(true);
        }
        return true;
    }

    public void delete() {
        getQpMapFragment().shortToast("Marker deleted: " + getQuietPlace().getComment());
        getMapMarker().remove();
        getCircle().remove();
        getQpMapFragment().removeQuietPlaceMapMarker(this);

        database_delete();

        // TODO: delete the geofence here?
    }

    private Circle addQuietPlaceCircle(QuietPlace quietPlace) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(
                new LatLng(quietPlace.getLatitude(), quietPlace.getLongitude())
        );
        circleOptions.radius(quietPlace.getRadius());
        circleOptions.strokeColor(Config.QP_CIRCLE_STROKE_COLOR);
        circleOptions.strokeWidth(Config.QP_CIRCLE_STROKE_WIDTH);
        if (!isSelected()) {
            circleOptions.fillColor(Config.QP_CIRCLE_FILL_COLOR);
        } else {
            circleOptions.fillColor(Config.QP_CIRCLE_SELECTED_FILL_COLOR);

        }

        return getQpMapFragment().getMap().addCircle(circleOptions);
    }

    private void removeQuietPlaceCircle() {
        Circle circle = getCircle();
        if (circle != null) {
            circle.remove();
        }
        setCircle(null);
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

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
        Log.w(TAG, "setting scale factor to: " + scaleFactor + " qp: " + getQuietPlace());
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {

        if (selected) {
            getQpMapFragment().unselectAllIfSingleSelectMode();
        }

        this.selected = selected;

        // Re-draw the circle to reflect the selection status
        updateCircle();

        getQpMapFragment().setSelectionMode();

        // center the camera on the selection unless unselecting
        if (selected) {
            centerCameraOnThis();

            getQpMapFragment().showInfoBox(true, this);

            // This QP is selected, so let it respond to scale gesture events.
            // getQpMapFragment().attachScaleListener(this);
        } else {

            // Unselecting this QP, so no longer listen for scale gestures.
            // Now we go back to regular map zooming.
            // getQpMapFragment().detachScaleListener();
        }

    }

    /**
     * Center the map camera on this map marker
     * TODO: reset zoom to match the radius?!
     */
    public void centerCameraOnThis() {
        getQpMapFragment().animateCamera(getMapMarker().getPosition());
    }

    /**
     * Called from onMarkerDrag when the marker moves, so
     * we can update the database and also the circle position.
     * <p/>
     * TODO: this could accept and set a new marker argument.
     * How we're doing it now implies that the underlying marker
     * object stays the same from onMarkerDrag and it just updates
     * the same object.
     *
     * @param doSave actually save the change to the database?
     *               can speed things up by not saving intermediate changes during a drag move
     */
    public void moveMarker(boolean doSave) {
        Marker marker = getMapMarker();

        // Update the circle around it.
        LatLng pos = marker.getPosition();
        getCircle().setCenter(pos);

        // Update the saved position in the database
        getQuietPlace().setLatitude(pos.latitude);
        getQuietPlace().setLongitude(pos.longitude);

        // Update the description string if any
        if (isSelected()) {
            getQpMapFragment().updateInfoString(getQuietPlace());
        }

        if (doSave) {
            database_save();
        } else {
            Log.d(TAG, "Intermediate QuietPlace move not saved to database.");
        }
    }

    /**
     * Sets a new radius in meters for this quiet place. Updates the visual
     * representation and saves the new size to the database.
     * @param radius in meters
     */
    public void setRadius(double radius) {
        getQuietPlace().setRadius(radius);
        updateCircle();
        getQpMapFragment().updateInfoString(getQuietPlace());
        database_save();
    }

    public double getRadius() {
        return getQuietPlace().getRadius();
    }

    /**
     * Re-draw the circle to reflect the selection status
     */
    public void updateCircle() {
        removeQuietPlaceCircle();
        setCircle(addQuietPlaceCircle(getQuietPlace()));
    }

    /**
     * Saves any changes to the QuietPlace object to the database.
     */
    private void database_save() {
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getQpMapFragment().getActivity());
        dataSource.open();
        setQuietPlace(dataSource.saveQuietPlace(getQuietPlace()));
        dataSource.close();
        Log.w(TAG, "Saved to database: " + getQuietPlace());
    }

    private void database_delete() {
        Log.w(TAG, "Deleting from database: " + getQuietPlace());
        QuietPlacesDataSource dataSource = new QuietPlacesDataSource(getQpMapFragment().getActivity());
        dataSource.open();
        dataSource.deleteQuietPlace(getQuietPlace());
        dataSource.close();
        Log.d(TAG, "Deletion complete.");

        // should we null out the QP object?!
    }

    /**
     * Increase the quiet place size (radius) by a dynamically determined increment.
     */
    public void grow() {
        setRadius(getRadius() + getResizeIncrement());
    }

    /**
     * Decrease the quiet place size (radius) by a dynamically determined increment.
     */
    public void shrink() {
        double radius = getRadius();
        double newRadius = radius - getResizeIncrement();
        if (newRadius < Config.QP_SIZE_FLOOR) {
            newRadius = Config.QP_SIZE_FLOOR;
        }
        setRadius(newRadius);

    }

    /**
     * Gets the distance (in meters) we should grow or shrink
     * a quiet place circle when the button is pressed. This
     * depends on the current scale (zoom level) of the map.
     *
     * @return distance in meters to change based on current map zoom level
     *
     */
    private double getResizeIncrement() {
        return getQpMapFragment().getSuggestedRadius() * Config.QP_RESIZE_INCREMENT_FACTOR;
    }


}
