package edu.utexas.quietplaces;

import android.app.Dialog;
import android.content.*;
import android.location.Location;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.support.v4.widget.DrawerLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, LocationClient.OnAddGeofencesResultListener {

    private static final String TAG = Config.PACKAGE_NAME + ".MainActivity";
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private QPMapFragment mapFragment;
    private HomeFragment homeFragment;
    private PlaceholderFragment placeholderFragment; // this is current a placeholder fragment, convert it to the History fragment
    private SettingsFragment settingsFragment;
    private AboutFragment aboutFragment;

    // Define an object that holds accuracy and frequency parameters
    private LocationRequest mLocationRequest;
    private LocationClient mLocationClient;

    private SharedPreferences sharedPrefs;

    private GoogleMap googleMap = null;
    private boolean mUpdatesRequested = false;
    private Location lastKnownLocation = null;

    private boolean haveAlreadyCenteredCamera = false;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private GeofenceRequester mGeofenceRequester;
    private GeofenceRemover mGeofenceRemover;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    /*
     * Use to set an expiration time for a geofence. After this amount
     * of time Location Services will stop tracking the geofence.
     * Remember to unregister a geofence when you're finished with it.
     * Otherwise, your app will use up battery. To continue monitoring
     * a geofence indefinitely, set the expiration time to
     * Geofence#NEVER_EXPIRE.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * android.text.format.DateUtils.HOUR_IN_MILLIS;

    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    private GeofenceSampleReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter mIntentFilter;

    // Store the list of geofences to remove
    private List<String> mGeofenceIdsToRemove;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preference defaults
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Get preference object
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);

        // Create a new broadcast receiver to receive updates from the listeners and service
        mBroadcastReceiver = new GeofenceSampleReceiver();

        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        List<Fragment> currentFragments = getSupportFragmentManager().getFragments();
        if (currentFragments == null || currentFragments.size() == 0) {
            mapFragment = getMapFragment();
            homeFragment = HomeFragment.newInstance(1);
            placeholderFragment = PlaceholderFragment.newInstance(3);
            settingsFragment = new SettingsFragment();
            aboutFragment = AboutFragment.newInstance(5);

            // Add all the fragments but only show the home one initially
            // Got this idea from: http://stackoverflow.com/questions/16461483/preserving-fragment-state
            // Main purpose of this is to avoid resetting the map state when you switch fragments
            // It's kind of ugly because you have to manually manage hiding and showing everything
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.frame_home, homeFragment)
                    .hide(homeFragment)
                    .add(R.id.frame_map, mapFragment)
                    .hide(mapFragment)
                    .add(R.id.frame_placeholder, placeholderFragment)
                    .hide(placeholderFragment)
                    .add(R.id.frame_about, aboutFragment)
                    .hide(aboutFragment)
                    .commit();

            getFragmentManager().beginTransaction()
                    .add(R.id.frame_settings, settingsFragment)
                    .hide(settingsFragment)
                    .commit();
        } else {
            // Load saved fragments
            mapFragment = (QPMapFragment) getSupportFragmentManager().findFragmentById(R.id.frame_map);
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.frame_home);
            placeholderFragment = (PlaceholderFragment) getSupportFragmentManager().findFragmentById(R.id.frame_placeholder);
            settingsFragment = (SettingsFragment) getFragmentManager().findFragmentById(R.id.frame_settings);
            aboutFragment = (AboutFragment) getSupportFragmentManager().findFragmentById(R.id.frame_about);
        }


        setContentView(R.layout.activity_main);

        // Create objects for all of our primary fragments

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up location updates.
        // TODO: make this a setting?
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                // LocationRequest.PRIORITY_HIGH_ACCURACY
                // LocationRequest.PRIORITY_LOW_POWER
        );
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(Config.LOCATION_UPDATE_INTERVAL_MS);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(Config.LOCATION_FASTEST_INTERVAL_MS);

        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        getFragmentManager().beginTransaction()
                .hide(settingsFragment).commit();

        switch (position) {
            case 0:
                transaction.hide(mapFragment)
                        .hide(placeholderFragment)
                        .hide(aboutFragment)
                        .show(homeFragment);
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                transaction.hide(homeFragment)
                        .hide(placeholderFragment)
                        .hide(aboutFragment)
                        .show(mapFragment);
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(aboutFragment)
                        .show(placeholderFragment);
                mTitle = getString(R.string.title_section3);
                break;
            case 3:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(aboutFragment)
                        .hide(placeholderFragment);

                getFragmentManager().beginTransaction()
                        .show(settingsFragment).commit();
                mTitle = getString(R.string.settings_activity_name);
                break;

            case 4:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(placeholderFragment)
                        .show(aboutFragment);
                mTitle = getString(R.string.title_section_about);
                break;

            default:
                shortToast("Unknown section - this is a bug");
        }

        transaction.commit();
    }

    SettingsFragment getSettingsFragment() {
        return settingsFragment;
    }

    QPMapFragment getMapFragment() {
        if (mapFragment != null) {
            return mapFragment;
        }
//        GoogleMapOptions options = new GoogleMapOptions();
//        options.mapType(GoogleMap.MAP_TYPE_HYBRID)
//                .compassEnabled(true);
//        mapFragment = SupportMapFragment.newInstance(options);
        // mapFragment.setRetainInstance(true);


        mapFragment = QPMapFragment.newInstance(2);
        setupMapIfNeeded();
        return mapFragment;
    }


    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkGooglePlayServicesAvailable()) {
            Log.e(TAG, "Can't find Google Play Services needed for maps and location.");
            longToast("Can't get Google Play Services. :-(");
            // Maybe quit at this point?
        }

        haveAlreadyCenteredCamera = false; /// good idea?
        setupMapIfNeeded();

        mUpdatesRequested = getPrefUsingLocation();

        // Register the broadcast receiver to receive status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);

    }

    // onDestroy is not defined either
/*
    @Override
    protected void onPause() {
        super.onPause();
    }
*/

    @Override
    protected void onStop() {
        super.onStop();

        // If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();

    }

    private void setupMapIfNeeded() {
        googleMap = getMapFragment().getMap();
        if (googleMap == null) {
            // longToast("Can't get map object. :-(");
            // This can happen if the user hasn't visited the map tab yet
            return;
        }

        // other map setup here

        googleMap.setMyLocationEnabled(true);

        if (lastKnownLocation != null) {
            updateUserLocationOnMap(lastKnownLocation);
        }
    }

    public void onSectionAttached(int number) {
/*
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.settings_activity_name);
                break;
        }
*/
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            // getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/


    public void onClickRinger(View view) {
        Switch ringerSwitch = (Switch) findViewById(R.id.switch_home_ringer);
        // String result = "Ringer turned off.";
        boolean ringerState = false;
        if (ringerSwitch.isChecked()) {
            // result = "Ringer turned on.";
            ringerState = true;
        }
        // shortToast(result);
        setRinger(ringerState);
    }

    public void shortToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    public void longToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    // Todo: support separate vibration setting?
    private void setRinger(boolean ringerState) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int ringerMode = AudioManager.RINGER_MODE_NORMAL;
        if (!ringerState) {
            ringerMode = AudioManager.RINGER_MODE_SILENT;
        }
        audioManager.setRingerMode(ringerMode);

    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    /**
     * Called if the device does not have Google Play Services installed.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            connectionStatusCode, MainActivity.this, REQUEST_GOOGLE_PLAY_SERVICES);
                    dialog.show();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to obtain Google Play!.", e);
                }

            }
        });
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        if (mUpdatesRequested) {
            shortToast("Requesting Location Services");
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        } else {
            shortToast("Location Services Disabled");
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        shortToast("Disconnected from Location Services. Please re-connect.");
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        REQUEST_GOOGLE_PLAY_SERVICES); // Not sure if this is right...
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showGooglePlayServicesAvailabilityErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
/*
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        shortToast(msg);
*/

        lastKnownLocation = location;

        if (!haveAlreadyCenteredCamera) {
            haveAlreadyCenteredCamera = true;
            updateUserLocationOnMap(location);
        }
    }

    private boolean getPrefUsingLocation() {
        return sharedPrefs.getBoolean(SettingsFragment.KEY_USE_LOCATION, false);
    }

    private void updateUserLocationOnMap(Location location) {
        // setupMapIfNeeded();
        if (googleMap == null) {
            Log.w(TAG, "Map doesn't exist so can't show user location");
            return;
        }
        float zoom = (float) 16.0; // a fairly tight zoom  (TODO: a setting?)
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), zoom)
        );
    }

    // Forward the button clicks from the action panel to the map fragment
    public void clickAddButton(View view) {
        getMapFragment().clickAddButton(view);
    }

    public void clickGrowButton(View view) {
        getMapFragment().clickGrowButton(view);
    }

    public void clickShrinkButton(View view) {
        getMapFragment().clickShrinkButton(view);
    }

    public void clickCenterButton(View view) {
        getMapFragment().clickCenterButton(view);
    }

    public void clickEditButton(View view) {
        getMapFragment().clickEditButton(view);
    }

    public void clickDeleteButton(View view) {
        getMapFragment().clickDeleteButton(view);
    }

    /*
     * Provide the implementation of
     * OnAddGeofencesResultListener.onAddGeofencesResult.
     * Handle the result of adding the geofences
     *
     */
    @Override
    public void onAddGeofencesResult(
            int statusCode, String[] geofenceRequestIds) {

        // If adding the geofences was successful
        if (LocationStatusCodes.SUCCESS == statusCode) {
            /*
             * Handle successful addition of geofences here.
             * You can send out a broadcast intent or update the UI.
             * geofences into the Intent's extended data.
             */
            Log.i(TAG, "Added geofence successfully. Status code: " + statusCode);
        } else {
            // If adding the geofences failed
            /*
             * Report errors here.
             * You can log the error using Log.e() or update
             * the UI.
             */
            Log.e(TAG, "Unable to create geofence. Status code: " + statusCode);
            shortToast("Error: unable to create geofence!");
        }
        // Turn off the in progress flag and disconnect the client
//        mInProgress = false;
//        mLocationClient.disconnect();
    }

    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceSampleReceiver extends BroadcastReceiver {
        /*
         * Define the required method for broadcast receivers
         * This method is invoked when a broadcast Intent triggers the receiver
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check the action code and determine what to do
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

                handleGeofenceError(context, intent);

                // Intent contains information about successful addition or removal of geofences
            } else if (
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                            ||
                            TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

                handleGeofenceStatus(context, intent);

                // Intent contains information about a geofence transition
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

                handleGeofenceTransition(context, intent);

                // The Intent contained an invalid action
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * If you want to display a UI message about adding or removing geofences, put it here.
         *
         * @param context A Context for this component
         * @param intent  The received broadcast Intent
         */
        private void handleGeofenceStatus(Context context, Intent intent) {

        }

        /**
         * Report geofence transitions to the UI
         *
         * @param context A Context for this component
         * @param intent  The Intent containing the transition
         */
        private void handleGeofenceTransition(Context context, Intent intent) {
            /*
             * If you want to change the UI when a transition occurs, put the code
             * here. The current design of the app uses a notification to inform the
             * user that a transition has occurred.
             */
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         *
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

            // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }
}
