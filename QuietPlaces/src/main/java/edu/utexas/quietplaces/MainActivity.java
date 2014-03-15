package edu.utexas.quietplaces;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.widget.DrawerLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = Config.APPNAME + ".MainActivity";
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private QPMapFragment mapFragment;
    private HomeFragment homeFragment;
    private PlaceholderFragment placeholderFragment; // this is current a placeholder empty fragment, convert to something!

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize preference defaults
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // Get preference object
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mapFragment = getMapFragment();
        homeFragment = HomeFragment.newInstance(1);
        placeholderFragment = PlaceholderFragment.newInstance(3);

        // Add all the fragments but only show the home one initially
        // Got this idea from: http://stackoverflow.com/questions/16461483/preserving-fragment-state
        // Main purpose of this is to avoid resetting the map state when you switch fragments
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container, homeFragment)
                .add(R.id.container, mapFragment)
                .add(R.id.container, placeholderFragment)
                .hide(mapFragment)
                .hide(placeholderFragment)
                .commit();


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
        switch (position) {
            case 0:
                transaction.hide(mapFragment)
                        .hide(placeholderFragment)
                        .show(homeFragment);
                mTitle = getString(R.string.title_section1);
                break;
            case 1:
                transaction.hide(homeFragment)
                        .hide(placeholderFragment)
                        .show(mapFragment);
                mTitle = getString(R.string.title_section2);
                break;
            default:
                transaction.hide(mapFragment)
                        .hide(homeFragment)
                        .show(placeholderFragment);
                mTitle = getString(R.string.title_section3);
                break;
        }

        transaction.commit();
    }

    private QPMapFragment getMapFragment() {
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
    }

    @Override
    protected void onStop() {
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
        super.onStop();

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
        }
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
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

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
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        shortToast(msg);

        lastKnownLocation = location;

        if (!haveAlreadyCenteredCamera) {
            haveAlreadyCenteredCamera = true;
            updateUserLocationOnMap(location);
        }
    }

    private boolean getPrefUsingLocation() {
        return sharedPrefs.getBoolean(SettingsActivity.KEY_USE_LOCATION, false);
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
                        new LatLng(location.getLatitude(), location.getLongitude()), zoom));
    }

    // Forward the "Add" button click to the fragment
    public void clickAddButton(View view) {
        getMapFragment().clickAddButton(view);
    }
}
