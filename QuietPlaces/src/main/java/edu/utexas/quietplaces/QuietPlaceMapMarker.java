package edu.utexas.quietplaces;

import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import edu.utexas.quietplaces.content_providers.QuietPlacesContentProvider;
import edu.utexas.quietplaces.fragments.QPMapFragment;

/**
 * Contains an association of a QuietPlace and a MapMarker.
 */
public class QuietPlaceMapMarker {
    private static final String TAG = Config.PACKAGE_NAME + ".QuietPlaceMapMarker";

    private QuietPlace quietPlace;
    private Marker mapMarker;
    private QPMapFragment qpMapFragment;
    private Circle circle;
    private Geofence geofence;
    private String geofenceId;
    private boolean selected;
    // private double scaleFactor;
    private boolean currentlyInside;
    // TODO: add a "use geofence" setting?

    QuietPlaceMapMarker() {
        currentlyInside = false;
        selected = false;
    }

    @Override
    public String toString() {
        return "Quiet Place Map Marker " + getQuietPlace().toString();
    }

    /**
     * Main function for creating new map markers
     *
     * @param quietPlace    the place to add
     * @param qpMapFragment the containing map fragment
     * @return a QuietPlaceMapMarker instance
     */
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

        String geofenceId = quietPlace.getGplace_id();
        if (geofenceId == null || geofenceId.length() == 0) {
            // no Place ID available, so use the basic database ID.
            geofenceId =Long.toString(quietPlace.getId());
        }
        setGeofenceId(geofenceId);


        MarkerOptions markerOptions = new MarkerOptions();
        LatLng latLng = new LatLng(quietPlace.getLatitude(), quietPlace.getLongitude());
        markerOptions.position(latLng);
        markerOptions.title(comment);
        markerOptions.draggable(true);

        // Draw a circle to represent the geofence
        // May need to hang on to this reference somehow for when
        // we need to adjust the radius size by gesture
        setCircle(addQuietPlaceCircle(quietPlace));

        // TODO: This is a vestige of attempted gesture scaling support and can probably be removed
        // setScaleFactor(quietPlace.getRadius());

        setGeofence();

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

    /**
     * Delete this Quiet Place from the database and remove its geofence.
     */
    public void delete() {
        String logMsg = getQuietPlace().getHistoryEventFormatted();
        Log.d(TAG, "Marker deleted: " + logMsg);

        boolean wasAutoAdded = getQuietPlace().isAutoadded();

        // needs to be before we delete the geofence
        getQpMapFragment().removeQuietPlaceMapMarker(this);

        // queues geofence for removal; call syncGeofences to activate changes
        removeGeofence();

        getMapMarker().remove();
        getCircle().remove();

        database_delete();

        if (!wasAutoAdded) {
            HistoryEvent.logEvent(
                    getQpMapFragment().getMyActivity(),
                    HistoryEvent.TYPE_PLACE_REMOVE,
                    logMsg
            );
        }

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
            if (quietPlace.isAutoadded()) {
                circleOptions.strokeColor(Config.QP_AUTO_CIRCLE_STROKE_COLOR);
                circleOptions.fillColor(Config.QP_AUTO_CIRCLE_FILL_COLOR);
            } else {
                circleOptions.fillColor(Config.QP_CIRCLE_FILL_COLOR);
            }
        } else {
            circleOptions.fillColor(Config.QP_CIRCLE_SELECTED_FILL_COLOR);
            circleOptions.strokeColor(Config.QP_CIRCLE_SELECTED_STROKE_COLOR);
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

/*
    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
        // Log.v(TAG, "setting scale factor to: " + scaleFactor + " qp: " + getQuietPlace());
    }
*/

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {

        if (selected) {
            getQpMapFragment().unselectAll();
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

    public boolean isCurrentlyInside() {
        return currentlyInside;
    }

    public void setCurrentlyInside(boolean currentlyInside) {
        this.currentlyInside = currentlyInside;
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
        if (doSave) {
            removeGeofence();
        }
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
            setGeofence();
            database_save();
            getQpMapFragment().syncGeofences();

            HistoryEvent.logEvent(
                    getQpMapFragment().getMyActivity(),
                    HistoryEvent.TYPE_PLACE_UPDATE,
                    getQuietPlace().getHistoryEventFormatted()
            );

        } else {
            Log.d(TAG, "Intermediate QuietPlace move not saved to database.");
        }

    }

    /**
     * Change the comment/name field of this quiet place with database save.
     *
     * @param comment new string name/comment for this quiet place
     */
    public void setComment(String comment) {
        getQuietPlace().setComment(comment);
        database_save();
        HistoryEvent.logEvent(
                getQpMapFragment().getMyActivity(),
                HistoryEvent.TYPE_PLACE_UPDATE,
                getQuietPlace().getHistoryEventFormatted()
        );
    }

    /**
     * Sets a new radius in meters for this quiet place. Updates the visual
     * representation and saves the new size to the database and syncs the geofence.
     *
     * @param radius in meters
     */
    public void setRadius(double radius) {
        removeGeofence();
        getQuietPlace().setRadius(radius);
        updateCircle();
        getQpMapFragment().updateInfoString(getQuietPlace());
        database_save();
        setGeofence();
        getQpMapFragment().syncGeofences();

        HistoryEvent.logEvent(
                getQpMapFragment().getMyActivity(),
                HistoryEvent.TYPE_PLACE_UPDATE,
                getQuietPlace().getHistoryEventFormatted()
        );

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
     * <p/>
     * We automatically convert any auto-add places to permanent places here
     * on the assumption that if they're changing it, they want to keep it.
     */
    private void database_save() {
        QuietPlace qp = getQuietPlace();
        if (qp == null) {
            Log.e(TAG, "Can't save QuietPlaceMapMarker with null QuietPlace");
            return;
        }

        // Ensure that if we're saving changes to an auto-add place
        // it becomes a permanent place.
        if (qp.isAutoadded()) {
            Log.w(TAG, "Converting autoadded QuietPlace to permanent mode: " + qp);
            qp.setAutoadded(false);
        }

        Log.v(TAG, "Saving to database: " + qp);
        qp = QuietPlacesContentProvider.saveQuietPlace(getQpMapFragment().getMyActivity(), qp);
        if (qp == null) {
            Log.e(TAG, "Unable to database_save quietplace!");
            return;
        }
        setQuietPlace(qp);
        Log.w(TAG, "Saved to database: " + qp);
    }

    private void database_delete() {
        Log.i(TAG, "Deleting from database: " + getQuietPlace());
        QuietPlacesContentProvider.deleteQuietPlace(getQpMapFragment().getMyActivity(), getQuietPlace());
        // Log.v(TAG, "Deletion complete.");

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
     */
    private double getResizeIncrement() {
        return getQpMapFragment().getSuggestedRadius() * Config.QP_RESIZE_INCREMENT_FACTOR;
    }

    public Geofence getGeofence() {
        return geofence;
    }

    public void setGeofence(Geofence geofence) {
        this.geofence = geofence;
    }

    /**
     * Activate the geofence for this quiet place based on current settings.
     * This actually just adds it to a queue; you have to call sendGeofenceAdditions
     * on the QPMapFragment
     */
    private void setGeofence() {
        // Create and add the geofence.
        geofence = buildGeofence();
        setGeofence(geofence);
        if (geofence == null) {
            Log.e(TAG, "Can't obtain geofence object to set.");
            return;
        }

        Log.i(TAG, "Queing Geofence ID: " + getGeofenceId());
        getQpMapFragment().queueGeofenceAdd(geofence);
    }

    /**
     * Queue the currently active geofence, if any, for removal.
     * Have to call sendGeofenceRemovals() on QPMapFragment to execute
     */
    private void removeGeofence() {
        if (geofence != null) {
            getQpMapFragment().queueGeofenceIdRemove(geofence.getRequestId());
            setGeofence(null); // we're nulling this before it's actually removed from Loc Svcs
        } else {
            Log.w(TAG, "Can't remove geofence because it's null on: " + getQuietPlace());
        }
    }


    /**
     * Returns a Geofence object corresponding to the current settings of this quiet place.
     *
     * @return
     */
    private Geofence buildGeofence() {
        QuietPlace quietPlace = getQuietPlace();
        if (quietPlace == null) {
            Log.e(TAG, "can't buildGeofence: getQuietPlace() is null");
            return null;
        }
        int transitionType = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;
        float radius = (float) getRadius();
        return new Geofence.Builder()
                .setRequestId(getGeofenceId())
                .setTransitionTypes(transitionType)
                .setCircularRegion(
                        quietPlace.getLatitude(),
                        quietPlace.getLongitude(),
                        radius)
                .setExpirationDuration(Config.GEOFENCE_MAX_LIFETIME_MS)
                .build();
    }

    public void enterGeofence() {
        setCurrentlyInside(true);

        Log.w(TAG, "Entered Geofence: " + getGeofenceId() + " qp: " + quietPlace);

        // SILENCE RINGER HERE....
        MainActivity mainActivity = (MainActivity) getQpMapFragment().getMyActivity();
        if (mainActivity == null) {
            Log.i(TAG, "MainActivity is null in enterGeofence()");
            return;
        }
        if (mainActivity.silenceDeviceFromGeofence()) {
            Log.i(TAG, "Entered geofence and silenced the ringer. Entered: " + quietPlace);

            // History event
            HistoryEvent.logEvent(
                    getQpMapFragment().getMyActivity(),
                    HistoryEvent.TYPE_PLACE_ENTER,
                    quietPlace.getHistoryEventFormatted()
            );

            // Notification bar.
            int transition = Geofence.GEOFENCE_TRANSITION_ENTER;
            mainActivity.sendGeofenceNotification(
                    transition,
                    Config.getTransitionString(mainActivity, transition),
                    Long.toString(getQuietPlace().getId()),
                    getNotificationSubtitle());
        } else {
            Log.i(TAG, "Entered geofence but ringer was already silenced. Entered: " + quietPlace);
        }

    }

    public void exitGeofence() {
        setCurrentlyInside(false);

        Log.w(TAG, "Exited Geofence: " + getGeofenceId() + " QP: " + quietPlace);

        // UNSILENCE RINGER HERE....
        // only if all other zones are clear also.
        if (getQpMapFragment().areWeInsideOtherGeofences(this)) {
            Log.i(TAG, "Exited geofence but we're still inside another geofence. Exited: " + quietPlace);
            return;
        }

        MainActivity mainActivity = (MainActivity) getQpMapFragment().getMyActivity();
        if (mainActivity != null && mainActivity.unsilenceDeviceFromGeofence()) {
            Log.i(TAG, "Exited geofence and re-activated the ringer. Exited: " + quietPlace);
            HistoryEvent.logEvent(
                    getQpMapFragment().getMyActivity(),
                    HistoryEvent.TYPE_PLACE_EXIT,
                    quietPlace.getHistoryEventFormatted()
            );

            // Notification bar.
            int transition = Geofence.GEOFENCE_TRANSITION_EXIT;
            mainActivity.sendGeofenceNotification(
                    transition,
                    Config.getTransitionString(mainActivity, transition),
                    Long.toString(getQuietPlace().getId()),
                    getNotificationSubtitle());

        } else {
            Log.w(TAG, "Exited geofence and the ringer was either already active or else we " +
                    "left it silent because we didn't silence in the first place. Exited gfence: " + quietPlace);
        }

    }

    private String getNotificationSubtitle() {
        return "QuietPlace: " + getQuietPlace().getComment();
    }

    public String getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(String geofenceId) {
        this.geofenceId = geofenceId;
    }
}
