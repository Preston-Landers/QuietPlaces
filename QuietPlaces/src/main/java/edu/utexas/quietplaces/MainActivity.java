package edu.utexas.quietplaces;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import edu.utexas.quietplaces.fragments.*;
import edu.utexas.quietplaces.receivers.LocationChangedReceiver;
import edu.utexas.quietplaces.receivers.PassiveLocationChangedReceiver;
import edu.utexas.quietplaces.services.EclairPlacesUpdateService;
import edu.utexas.quietplaces.services.PlacesUpdateService;
import edu.utexas.quietplaces.utils.LocationUpdateRequester;
import edu.utexas.quietplaces.utils.PlatformSpecificImplementationFactory;
import edu.utexas.quietplaces.utils.base.ILastLocationFinder;
import edu.utexas.quietplaces.utils.base.SharedPreferenceSaver;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        LocationListener, LocationClient.OnAddGeofencesResultListener {

    private static final String TAG = Config.PACKAGE_NAME + ".MainActivity";
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    // The rest of our fragments - each is a tab in the navigation
    private QPMapFragment mapFragment;
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;
    private AboutFragment aboutFragment;

    // Define an object that holds accuracy and frequency parameters
//    private LocationRequest mLocationRequest;
//    private LocationClient mLocationClient;

    protected LocationManager locationManager;
    protected SharedPreferences.Editor prefsEditor;
    protected SharedPreferenceSaver sharedPreferenceSaver;

    protected Criteria criteria;
    protected ILastLocationFinder lastLocationFinder;
    protected LocationUpdateRequester locationUpdateRequester;
    protected PendingIntent locationListenerPendingIntent;
    protected PendingIntent locationListenerPassivePendingIntent;

    private SharedPreferences prefs;

    private GoogleMap googleMap = null;
    private boolean mUpdatesRequested = false;
    private Location lastKnownLocation = null;

    private boolean haveAlreadyCenteredCamera = false;

    private boolean haveRegisteredBroadcastReceiver = false;

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
    private GeofenceReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter mIntentFilter;

    // We don't want to un-silence if the device was manually silenced before
    // entering the quiet zone. So keep track of whether we were the ones to engage
    // the silence.
    private boolean weSilencedTheDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize preference defaults
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Get preference object
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();

        // Instantiate a SharedPreferenceSaver class based on the available platform version.
        // This will be used to save shared preferences
        sharedPreferenceSaver = PlatformSpecificImplementationFactory.getSharedPreferenceSaver(this);

        // Save that we've been run once.
        prefsEditor.putBoolean(PlacesConstants.SP_KEY_RUN_ONCE, true);
        sharedPreferenceSaver.savePreferences(prefsEditor, false);

        // Specify the Criteria to use when requesting location updates while the application is Active
        criteria = new Criteria();
        if (PlacesConstants.USE_GPS_WHEN_ACTIVITY_VISIBLE) {
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
        } else {
            criteria.setPowerRequirement(Criteria.POWER_LOW);
        }

        // Setup the location update Pending Intents
        Intent activeIntent = new Intent(this, LocationChangedReceiver.class);
        locationListenerPendingIntent = PendingIntent.getBroadcast(this, 0, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent passiveIntent = new Intent(this, PassiveLocationChangedReceiver.class);
        locationListenerPassivePendingIntent = PendingIntent.getBroadcast(this, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        locationManager.removeUpdates(locationListenerPassivePendingIntent);

        // Instantiate a LastLocationFinder class.
        // This will be used to find the last known location when the application starts.
        lastLocationFinder = PlatformSpecificImplementationFactory.getLastLocationFinder(this);
        lastLocationFinder.setChangedLocationListener(oneShotLastLocationUpdateListener);

        // Instantiate a Location Update Requester class based on the available platform version.
        // This will be used to request location updates.
        locationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(locationManager);


        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);

        // Create a new broadcast receiver to receive updates from the listeners and service
        mBroadcastReceiver = new GeofenceReceiver();

        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

        // Action for broadcast Intents when we perform a geofence transition.
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        List<Fragment> currentFragments = getSupportFragmentManager().getFragments();
        if (currentFragments == null || currentFragments.size() == 0) {
            mapFragment = getMapFragment();       // TODO: check for null map here... no Google Play Services?
            homeFragment = HomeFragment.newInstance(1);
            historyFragment = HistoryFragment.newInstance(3);
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
                    .add(R.id.frame_history, historyFragment)
                    .hide(historyFragment)
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
            historyFragment = (HistoryFragment) getSupportFragmentManager().findFragmentById(R.id.frame_history);
            settingsFragment = (SettingsFragment) getFragmentManager().findFragmentById(R.id.frame_settings);
            aboutFragment = (AboutFragment) getSupportFragmentManager().findFragmentById(R.id.frame_about);
        }


        // Inflate the layout
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
        /*
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(
                // LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                // LocationRequest.PRIORITY_HIGH_ACCURACY
                LocationRequest.PRIORITY_LOW_POWER
        );

        // Set the normal update interval
        mLocationRequest.setInterval(Config.LOCATION_UPDATE_INTERVAL_MS);
        // Set the fastest accepted update interval
        mLocationRequest.setFastestInterval(Config.LOCATION_FASTEST_INTERVAL_MS);

        mLocationClient = new LocationClient(this, this, this);
        */
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
                        .hide(historyFragment)
                        .hide(aboutFragment)
                        .show(homeFragment);
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                transaction.hide(homeFragment)
                        .hide(historyFragment)
                        .hide(aboutFragment)
                        .show(mapFragment);
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(aboutFragment)
                        .show(historyFragment);
                mTitle = getString(R.string.title_section3);
                break;
            case 3:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(aboutFragment)
                        .hide(historyFragment);

                getFragmentManager().beginTransaction()
                        .show(settingsFragment).commit();
                mTitle = getString(R.string.settings_activity_name);
                break;

            case 4:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .hide(historyFragment)
                        .show(aboutFragment);
                mTitle = getString(R.string.title_section_about);
                break;

            default:
                shortToast("Unknown section - this is a bug");
        }

        transaction.commit();
    }

    public SettingsFragment getSettingsFragment() {
        return settingsFragment;
    }

    public QPMapFragment getMapFragment() {
        if (mapFragment != null) {
            return mapFragment;
        }
//        GoogleMapOptions options = new GoogleMapOptions();
//        options.mapType(GoogleMap.MAP_TYPE_HYBRID)
//                .compassEnabled(true);
//        mapFragment = SupportMapFragment.newInstance(options);
        // mapFragment.setRetainInstance(true);

        if (!servicesConnected()) {
            Log.e(TAG, "Can't get Google Play services to initialize map.");
            return null;
        }

        mapFragment = QPMapFragment.newInstance(2);
        setupMapIfNeeded();
        return mapFragment;
    }


    @Override
    protected void onStart() {
        super.onStart();
        // mLocationClient.connect();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (!checkGooglePlayServicesAvailable()) {
            Log.e(TAG, "Can't find Google Play Services needed for maps and location.");
            longToast("Can't get Google Play Services. :-(");
            // Maybe quit at this point?
        }

        haveAlreadyCenteredCamera = false; /// good idea?
        setupMapIfNeeded();
        mUpdatesRequested = getPrefUsingLocation();

        /// FROM LOCATION BEST PRACTICES

        // Commit shared preference that says we're in the foreground.
        prefsEditor.putBoolean(PlacesConstants.EXTRA_KEY_IN_BACKGROUND, false);
        sharedPreferenceSaver.savePreferences(prefsEditor, false);

        // Get the last known location (and optionally request location updates) and
        // update the place list.
        if (mUpdatesRequested) {
            boolean followLocationChanges = prefs.getBoolean(PlacesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, true);
            getLocationAndUpdatePlaces(followLocationChanges);
        }


        // Register the broadcast receiver to receive geofence status updates
        if (!haveRegisteredBroadcastReceiver) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
            haveRegisteredBroadcastReceiver = true;
        }


    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        // disableLocationUpdates();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // Commit shared preference that says we're in the background.
        prefsEditor.putBoolean(PlacesConstants.EXTRA_KEY_IN_BACKGROUND, true);
        sharedPreferenceSaver.savePreferences(prefsEditor, false);

        // Stop listening for location updates when the Activity is inactive.
        // NOTE: this was in the Location Best Practices app, but having
        // location and GPlaces updates in the background is crucial to our
        // apps's functionality with automatic place discovery.  However this could
        // impact the battery....
        // We still do it in onDestroy().

        disableLocationUpdates();

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

//        // If the client is connected
//        if (mLocationClient.isConnected()) {
//            /*
//             * Remove location updates for a listener.
//             * The current Activity is the listener, so
//             * the argument is "this".
//             */
//            mLocationClient.removeLocationUpdates(this);
//        }
//        /*
//         * After disconnect() is called, the client is
//         * considered "dead".
//         */
//        mLocationClient.disconnect();

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
        if (ringerSwitch.isChecked()) {
            unsilenceDevice();
        } else {
            silenceDevice();
        }
    }

    /**
     * Silence the device according to current preference (silent or vibrate).
     *
     * @return false if the device was already silent, true if we engaged silent mode.
     */
    public boolean silenceDevice() {
        boolean wasSilent = isRingerSilentOrVibrate();
        if (wasSilent) {
            Log.d(TAG, "device was already silent when silenceDevice() was called.");
            return false;
        }
        Switch ringerSwitch = (Switch) findViewById(R.id.switch_home_ringer);
        if (ringerSwitch != null) {
            ringerSwitch.setChecked(false);
        }
        setRinger(false);
        weSilencedTheDevice = true;
        return true;
    }

    /**
     * Restore normal ringer mode.
     *
     * @return false if the ringer was already in normal mode, otherwise true
     */
    public boolean unsilenceDevice() {
        boolean wasSilent = isRingerSilentOrVibrate();
        if (!wasSilent) {
            Log.d(TAG, "device was already in normal ringer mode when unsilenceDevice() was called.");
            return false;
        }
        Switch ringerSwitch = (Switch) findViewById(R.id.switch_home_ringer);
        if (ringerSwitch != null) {
            ringerSwitch.setChecked(true);
        }
        setRinger(true);
        weSilencedTheDevice = false;
        return true;
    }

    /**
     * Restore normal ringer mode, but only if this app had put it in silent mode.
     *
     * @return false if the ringer was already in normal mode, or we didn't silence it before.
     */
    public boolean unsilenceDeviceIfWeSilenced() {
        if (weSilencedTheDevice) {
            return unsilenceDevice();
        }
        Log.i(TAG, "Unsilence request skipped because we did not silence the device.");
        return false;
    }


    // Todo: support separate vibration setting?
    private void setRinger(boolean ringerState) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int ringerMode = AudioManager.RINGER_MODE_NORMAL;
        if (!ringerState) {
            if (getPrefUsingVibrate()) {
                ringerMode = AudioManager.RINGER_MODE_VIBRATE;
                Log.i(TAG, "Setting ringer state to VIBRATE");
            } else {
                ringerMode = AudioManager.RINGER_MODE_SILENT;
                Log.i(TAG, "Setting ringer state to SILENT");
            }
        } else {
            Log.i(TAG, "Setting ringer state to NORMAL");
        }
        audioManager.setRingerMode(ringerMode);

    }

    private boolean isRingerSilentOrVibrate() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        return (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE);
    }


    public void shortToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    public void longToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
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
/*
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        if (mUpdatesRequested) {
            // shortToast("Requesting Location Services");
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        } else {
            shortToast("Location Services Disabled");
        }
    }
*/

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
/*
    @Override
    public void onDisconnected() {
        // Display the connection status
        shortToast("Disconnected from Location Services. Please re-connect.");
    }
*/

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        /*
//         * Google Play services can resolve some errors it detects.
//         * If the error has a resolution, try sending an Intent to
//         * start a Google Play services activity that can resolve
//         * error.
//         */
//        if (connectionResult.hasResolution()) {
//            try {
//                // Start an Activity that tries to resolve the error
//                connectionResult.startResolutionForResult(
//                        this,
//                        REQUEST_GOOGLE_PLAY_SERVICES); // Not sure if this is right...
//                /*
//                * Thrown if Google Play services canceled the original
//                * PendingIntent
//                */
//            } catch (IntentSender.SendIntentException e) {
//                // Log the error
//                e.printStackTrace();
//            }
//        } else {
//            /*
//             * If no resolution is available, display a dialog to the
//             * user with the error.
//             */
//            showGooglePlayServicesAvailabilityErrorDialog(connectionResult.getErrorCode());
//        }
//    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        // TODO: handle this w/ new location stuff
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
        return prefs.getBoolean(SettingsFragment.KEY_USE_LOCATION, false);
    }

    private boolean getPrefUsingVibrate() {
        return prefs.getBoolean(SettingsFragment.KEY_USE_VIBRATE, false);
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
        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {
            Log.e(TAG, "Can't click add button - no Google Play Services!");
            return;
        }

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

    public void clickClearHistoryButton(View view) {
        historyFragment.clickClearHistoryButton(view);
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

    public boolean requestGeofences(List<Geofence> geofenceList) {
        try {
            mGeofenceRequester.addGeofences(geofenceList);
            return true;
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Unable to set geofences.", e);
            return false;
        }
    }

    public boolean removeGeofences(List<String> geofenceIdList) {
        mGeofenceRemover.removeGeofencesById(geofenceIdList);
        return true;
    }

    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceReceiver extends BroadcastReceiver {
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
            // TODO: I'm not sure this code will be called.
            Log.i(TAG, "Geofence was added.");
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

            String[] geofenceIds = intent.getStringArrayExtra(GeofenceUtils.EXTRA_GEOFENCE_IDS);
            boolean entered = intent.getBooleanExtra(GeofenceUtils.EXTRA_GEOFENCE_ENTERED, true);

            // Log.i(TAG, "Geofence TRANSITION!");
            getMapFragment().handleGeofenceTransitions(geofenceIds, entered);
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         *
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(TAG, msg);
            longToast(msg);
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
     * <p/>
     * TODO: should be calling this when initially creating the map fragment.
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


    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     *
     * @param transitionType The type of transition that occurred.
     */
    protected void sendGeofenceNotification(int transition, String transitionType, String ids, String subtext) {

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent =
                new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int icon = HistoryEvent.ICON_PLACE_ENTER;
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            icon = HistoryEvent.ICON_PLACE_EXIT;
        }

        // Set the notification contents
        builder.setSmallIcon(icon)
                .setContentTitle(
                        getString(R.string.geofence_transition_notification_title,
                                transitionType, ids)
                )
                .setContentText(subtext)
                .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Update the list of nearby places centered on the specified Location, within the specified radius.
     * This will start the {@link edu.utexas.quietplaces.services.PlacesUpdateService}
     * that will poll the underlying web service.
     *
     * @param location     Location
     * @param radius       Radius (meters)
     * @param forceRefresh Force Refresh
     */
    protected void updatePlaces(Location location, int radius, boolean forceRefresh) {
        if (location != null) {
            Log.d(TAG, "Updating place list.");
            // Start the PlacesUpdateService. Note that we use an action rather than specifying the
            // class directly. That's because we have different variations of the Service for different
            // platform versions.
            Intent updateServiceIntent = new Intent(this, PlacesConstants.SUPPORTS_ECLAIR ? EclairPlacesUpdateService.class : PlacesUpdateService.class);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_LOCATION, location);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_RADIUS, radius);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, forceRefresh);
            startService(updateServiceIntent);
        } else
            Log.d(TAG, "Updating place list for: No Previous Location Found");
    }

    /**
     * Find the last known location (using a {@link edu.utexas.quietplaces.utils.GingerbreadLastLocationFinder})
     * and updates theplace list accordingly.
     *
     * @param updateWhenLocationChanges Request location updates
     */
    protected void getLocationAndUpdatePlaces(boolean updateWhenLocationChanges) {
        // This isn't directly affecting the UI, so put it on a worker thread.
        AsyncTask<Void, Void, Void> findLastLocationTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Find the last known location, specifying a required accuracy of within the min distance between updates
                // and a required latency of the minimum time required between updates.
                Location lastKnownLocation = lastLocationFinder.getLastBestLocation(PlacesConstants.MAX_DISTANCE,
                        System.currentTimeMillis() - PlacesConstants.MAX_TIME);

                // Update the place list based on the last known location within a defined radius.
                // Note that this is *not* a forced update. The Place List Service has settings to
                // determine how frequently the underlying web service should be pinged. This function
                // is called everytime the Activity becomes active, so we don't want to flood the server
                // unless the location has changed or a minimum latency or distance has been covered.
                // TODO Modify the search radius based on user settings?
                updatePlaces(lastKnownLocation, PlacesConstants.DEFAULT_RADIUS, false);
                return null;
            }
        };
        findLastLocationTask.execute();

        // If we have requested location updates, turn them on here.
        toggleUpdatesWhenLocationChanges(updateWhenLocationChanges);
    }

    /**
     * Choose if we should receive location updates.
     *
     * @param updateWhenLocationChanges Request location updates
     */
    protected void toggleUpdatesWhenLocationChanges(boolean updateWhenLocationChanges) {
        // Save the location update status in shared preferences
        prefsEditor.putBoolean(PlacesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, updateWhenLocationChanges);
        sharedPreferenceSaver.savePreferences(prefsEditor, true);

        // Start or stop listening for location changes
        if (updateWhenLocationChanges)
            requestLocationUpdates();
        else
            disableLocationUpdates();
    }

    /**
     * Start listening for location updates.
     */
    protected void requestLocationUpdates() {
        // Normal updates while activity is visible.
        locationUpdateRequester.requestLocationUpdates(PlacesConstants.MAX_TIME, PlacesConstants.MAX_DISTANCE, criteria, locationListenerPendingIntent);

        // Passive location updates from 3rd party apps when the Activity isn't visible.
        locationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.PASSIVE_MAX_TIME, PlacesConstants.PASSIVE_MAX_DISTANCE, locationListenerPassivePendingIntent);

        // Register a receiver that listens for when the provider I'm using has been disabled.
        IntentFilter intentFilter = new IntentFilter(PlacesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
        registerReceiver(locProviderDisabledReceiver, intentFilter);

        // Register a receiver that listens for when a better provider than I'm using becomes available.
        String bestProvider = locationManager.getBestProvider(criteria, false);
        String bestAvailableProvider = locationManager.getBestProvider(criteria, true);
        if (bestProvider != null && !bestProvider.equals(bestAvailableProvider)) {
            locationManager.requestLocationUpdates(bestProvider, 0, 0, bestInactiveLocationProviderListener, getMainLooper());
        }
    }

    /**
     * Stop listening for location updates
     */
    protected void disableLocationUpdates() {
        Log.d(TAG, "disabling location updates.");
        unregisterReceiver(locProviderDisabledReceiver);
        locationManager.removeUpdates(locationListenerPendingIntent);
        locationManager.removeUpdates(bestInactiveLocationProviderListener);
        if (isFinishing()) {
            lastLocationFinder.cancel();
        }

        if (PlacesConstants.DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT && isFinishing()) {
            locationManager.removeUpdates(locationListenerPassivePendingIntent);
        } else {
            // https://code.google.com/p/android-protips-location/issues/detail?id=5
            locationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.PASSIVE_MAX_TIME,
                    PlacesConstants.PASSIVE_MAX_DISTANCE, locationListenerPassivePendingIntent);
        }
    }

    /**
     * One-off location listener that receives updates from the {@link edu.utexas.quietplaces.utils.GingerbreadLastLocationFinder}.
     * This is triggered where the last known location is outside the bounds of our maximum
     * distance and latency.
     */
    protected android.location.LocationListener oneShotLastLocationUpdateListener = new android.location.LocationListener() {
        public void onLocationChanged(Location l) {
            updatePlaces(l, PlacesConstants.DEFAULT_RADIUS, true);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }
    };

    /**
     * If the best Location Provider (usually GPS) is not available when we request location
     * updates, this listener will be notified if / when it becomes available. It calls
     * requestLocationUpdates to re-register the location listeners using the better Location
     * Provider.
     */
    protected android.location.LocationListener bestInactiveLocationProviderListener = new android.location.LocationListener() {
        public void onLocationChanged(Location l) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
            // Re-register the location listeners using the better Location Provider.
            requestLocationUpdates();
        }
    };

    /**
     * If the Location Provider we're using to receive location updates is disabled while the
     * app is running, this Receiver will be notified, allowing us to re-register our Location
     * Receivers using the best available Location Provider is still available.
     */
    protected BroadcastReceiver locProviderDisabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean providerDisabled = !intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false);
            // Re-register the location listeners using the best available Location Provider.
            if (providerDisabled)
                requestLocationUpdates();
        }
    };

}
