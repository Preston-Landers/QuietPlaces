/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utexas.quietplaces.services;

import android.app.IntentService;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import edu.utexas.quietplaces.Config;
import edu.utexas.quietplaces.PlacesConstants;
import edu.utexas.quietplaces.content_providers.PlaceDetailsContentProvider;
import edu.utexas.quietplaces.content_providers.PlacesContentProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Service that requests a list of nearby locations from the underlying web service.
 */
public class PlacesUpdateService extends IntentService {

    protected static String TAG = Config.PACKAGE_NAME + ".PlacesUpdateService";

    protected ContentResolver contentResolver;
    protected SharedPreferences prefs;
    protected Editor prefsEditor;
    protected ConnectivityManager cm;
    protected boolean lowBattery = false;
    protected boolean mobileData = false;
    protected int prefetchCount = 0;

    public PlacesUpdateService() {
        super(TAG);
        setIntentRedeliveryMode(false);
    }

    /**
     * Set the Intent Redelivery mode to true to ensure the Service starts "Sticky"
     * Defaults to "true" on legacy devices.
     */
    protected void setIntentRedeliveryMode(boolean enable) {
    }

    /**
     * Returns battery status. True if less than 10% remaining.
     *
     * @param battery Battery Intent
     * @return Battery is low
     */
    protected boolean getIsLowBattery(Intent battery) {
        float pctLevel = (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
                battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return pctLevel < 0.15;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        contentResolver = getContentResolver();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsEditor = prefs.edit();
    }

    /**
     * {@inheritDoc}
     * Checks the battery and connectivity state before removing stale venues
     * and initiating a server poll for new venues around the specified
     * location within the given radius.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Check if we're running in the foreground, if not, check if
        // we have permission to do background updates.
        boolean backgroundAllowed = cm.getBackgroundDataSetting();
        boolean inBackground = prefs.getBoolean(PlacesConstants.EXTRA_KEY_IN_BACKGROUND, true);

        if (!backgroundAllowed && inBackground) return;

        // Extract the location and radius around which to conduct our search.
        Location location = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
        int radius = PlacesConstants.DEFAULT_RADIUS;

        Bundle extras = intent.getExtras();
        if (intent.hasExtra(PlacesConstants.EXTRA_KEY_LOCATION)) {
            location = (Location) (extras.get(PlacesConstants.EXTRA_KEY_LOCATION));
            radius = extras.getInt(PlacesConstants.EXTRA_KEY_RADIUS, PlacesConstants.DEFAULT_RADIUS);
        }

        // Check if we're in a low battery situation.
        IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, batIntentFilter);
        lowBattery = getIsLowBattery(battery);

        // Check if we're connected to a data network, and if so - if it's a mobile network.
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        mobileData = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;

        // If we're not connected, enable the connectivity receiver and disable the location receiver.
        // There's no point trying to poll the server for updates if we're not connected, and the
        // connectivity receiver will turn the location-based updates back on once we have a connection.
        if (!isConnected) {
            Log.w(TAG, "Not connected!");
        } else {
            // If we are connected check to see if this is a forced update (typically triggered
            // when the location has changed).
            boolean doUpdate = intent.getBooleanExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, false);

            // If it's not a forced update (for example from the Activity being restarted) then
            // check to see if we've moved far enough, or there's been a long enough delay since
            // the last update and if so, enforce a new update.
            if (!doUpdate) {
                // Retrieve the last update time and place.
                long lastTime = prefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_TIME, Long.MIN_VALUE);
                long lastLat = prefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LAT, Long.MIN_VALUE);
                long lastLng = prefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LNG, Long.MIN_VALUE);
                Location lastLocation = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
                lastLocation.setLatitude(lastLat);
                lastLocation.setLongitude(lastLng);

                if (location == null) {
                    Log.w(TAG, "Location is null...");
                }
                // If update time and distance bounds have been passed, do an update.
                else if ((lastTime < System.currentTimeMillis() - PlacesConstants.MAX_TIME) ||
                        (lastLocation.distanceTo(location) > PlacesConstants.MAX_DISTANCE)) {
                    Log.i(TAG, "Time/distance bounds passed on places update");
                    doUpdate = true;
                } else {
                    Log.d(TAG, "Time/distance bounds not passed on places update");
                }
            }

            if (location == null) {
                Log.e(TAG, "null location in onHandleIntent");
            } else if (doUpdate) {
                // Refresh the prefetch count for each new location.
                prefetchCount = 0;
                // Remove the old locations - TODO: we flush old locations, but if the next request
                // fails we are left high and dry
                removeOldLocations(location, radius);
                // Hit the server for new venues for the current location.
                refreshPlaces(location, radius, null);

                // Tell the main activity about the new results.
                Intent placesUpdatedIntent = new Intent(Config.ACTION_PLACES_UPDATED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(placesUpdatedIntent);

            } else {
                Log.i(TAG, "Place List is fresh: Not refreshing");
            }

            // Retry any queued checkins.
/*
            Intent checkinServiceIntent = new Intent(this, PlaceCheckinService.class);
            startService(checkinServiceIntent);
*/
        }
        Log.d(TAG, "Place List Download Service Complete");
    }

    /**
     * Polls the underlying service to return a list of places within the specified
     * radius of the specified Location.
     *
     * @param location Location
     * @param radius   Radius
     */
    protected void refreshPlaces(Location location, int radius, String page_token) {
        if (location == null) {
            Log.e(TAG, "Null location in refreshPlaces");
            return;
        }
        // Log to see if we'll be prefetching the details page of each new place.
        if (mobileData) {
            Log.d(TAG, "Not prefetching due to being on mobile");
        } else if (lowBattery) {
            Log.d(TAG, "Not prefetching due to low battery");
        }

        long currentTime = System.currentTimeMillis();
        URL url;

        String placeTypes = prefs.getString(PlacesConstants.SP_KEY_API_PLACE_TYPES, PlacesConstants.SP_KEY_API_PLACE_TYPES_DEFAULT);
        Log.v(TAG, "Doing places search with types: " + placeTypes);


        try {
            // TODO Replace this with a URI to your own service.
            String locationStr = location.getLatitude() + "," + location.getLongitude();
            String baseURI = PlacesConstants.PLACES_LIST_BASE_URI;

            String placesFeed;
            if (page_token != null && page_token.length() > 0) {
                // Other params are actually ignored here.
                placesFeed = baseURI +
                        PlacesConstants.getPlacesAPIKey(this, true) +
                        "&pagetoken=" + page_token;
                        ;
            } else {
                placesFeed = baseURI +
                        "&types=" + placeTypes +
                        "&location=" + locationStr +
                        "&radius=" + radius +
                        PlacesConstants.getPlacesAPIKey(this, true);
            }
            url = new URL(placesFeed);

            Log.w(TAG, "HTTP request: " + url);

            // Open the connection
            URLConnection connection = url.openConnection();
            HttpsURLConnection httpConnection = (HttpsURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Use the XML Pull Parser to extract each nearby location.
                // TODO Replace the XML parsing to extract your own place list.
                InputStream in = httpConnection.getInputStream();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                int placesAddedThisRequest = 0;

                String next_page_token = "";

                xpp.setInput(in, null);
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("next_page_token")) {
                        next_page_token = xpp.nextText();
                    }
                    else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("result")) {
                        eventType = xpp.next();
                        String id = "";
                        String name = "";
                        String vicinity = "";
                        String types = "";
                        String locationLat = "";
                        String locationLng = "";
                        String viewport = "";
                        String icon = "";
                        String reference = "";
                        while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("result"))) {
                            if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("name"))
                                name = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("vicinity"))
                                vicinity = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("type"))
                                types = types == "" ? xpp.nextText() : types + " " + xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("lat"))
                                locationLat = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("lng"))
                                locationLng = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("icon"))
                                icon = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("reference"))
                                reference = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("id"))
                                id = xpp.nextText();
                            else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("next_page_token"))
                                next_page_token = xpp.nextText();
                            eventType = xpp.next();
                        }


                        Location placeLocation = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
                        placeLocation.setLatitude(Double.valueOf(locationLat));
                        placeLocation.setLongitude(Double.valueOf(locationLng));

                        Log.v(TAG, "Found place: " +
                                        " location: " + location +
                                        " id: " + id +
                                        " name: " + name +
                                        " vicinity: " + vicinity +
                                        " types: " + types +
                                        " ref: " + reference
                        );

                        if (!next_page_token.equals("")) {
                            Log.e(TAG, "WARNING: unhandled next_page_token from Places search " + next_page_token);
                        }

                        // Add each new place to the Places Content Provider
                        addPlace(location, id, name, vicinity, types, placeLocation, viewport, icon, reference, currentTime);
                        placesAddedThisRequest++;
                    }
                    eventType = xpp.next();
                }

                if (placesAddedThisRequest > 0) {
                    Log.i(TAG, "Found " + placesAddedThisRequest + " places this request.");

                    if (!next_page_token.equals("")) {
                        // TODO: we should check for INVALID_RESULT and retry after a shorter wait
                        // Currently, if this wait is too long, we waste time, but if it's too short, we don't get the next page.
                        Log.d(TAG, "Sleeping before fetching next page. Sleep interval (ms): " + PlacesConstants.PLACES_NEXT_PAGE_INTERVAL_MS);
                        SystemClock.sleep(PlacesConstants.PLACES_NEXT_PAGE_INTERVAL_MS);
                        Log.i(TAG, "Fetching next page of places results.");
                        refreshPlaces(location, radius, next_page_token);
                    }
                } else {
                    Log.w(TAG, "Found 0 places this request.");
                }

                // Remove places from the PlacesContentProviderlist that aren't from this update.
                String where = PlaceDetailsContentProvider.KEY_LAST_UPDATE_TIME + " < " + currentTime;
                contentResolver.delete(PlacesContentProvider.CONTENT_URI, where, null);

                // Save the last update time and place to the Shared Preferences.
                prefsEditor.putLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LAT, (long) location.getLatitude());
                prefsEditor.putLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LNG, (long) location.getLongitude());
                prefsEditor.putLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_TIME, System.currentTimeMillis());
                prefsEditor.commit();
            } else
                Log.e(TAG, responseCode + ": " + httpConnection.getResponseMessage());

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (XmlPullParserException e) {
            Log.e(TAG, e.getMessage());
        } finally {
        }
    }

    /**
     * Adds the new place to the {@link PlacesContentProvider} using the values passed in.
     * TODO Update this method to accept and persist the place information your service provides.
     *
     * @param currentLocation Current location
     * @param id              Unique identifier
     * @param name            Name
     * @param vicinity        Vicinity
     * @param types           Types
     * @param location        Location
     * @param viewport        Viewport
     * @param icon            Icon
     * @param reference       Reference
     * @param currentTime     Current time
     * @return Successfully added
     */
    protected boolean addPlace(Location currentLocation, String id, String name, String vicinity, String types, Location location, String viewport, String icon, String reference, long currentTime) {
        // Contruct the Content Values
        ContentValues values = new ContentValues();
        values.put(PlacesContentProvider.KEY_ID, id);
        values.put(PlacesContentProvider.KEY_NAME, name);
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        values.put(PlacesContentProvider.KEY_LOCATION_LAT, lat);
        values.put(PlacesContentProvider.KEY_LOCATION_LNG, lng);
        values.put(PlacesContentProvider.KEY_VICINITY, vicinity);
        values.put(PlacesContentProvider.KEY_TYPES, types);
        values.put(PlacesContentProvider.KEY_VIEWPORT, viewport);
        values.put(PlacesContentProvider.KEY_ICON, icon);
        values.put(PlacesContentProvider.KEY_REFERENCE, reference);
        values.put(PlacesContentProvider.KEY_LAST_UPDATE_TIME, currentTime);

        // Calculate the distance between the current location and the venue's location
        float distance = 0;
        if (currentLocation != null && location != null)
            distance = currentLocation.distanceTo(location);
        values.put(PlacesContentProvider.KEY_DISTANCE, distance);

        // Update or add the new place to the PlacesContentProvider
        String where = PlacesContentProvider.KEY_ID + " = '" + id + "'";
        boolean result = false;
        try {
            if (contentResolver.update(PlacesContentProvider.CONTENT_URI, values, where, null) == 0) {
                if (contentResolver.insert(PlacesContentProvider.CONTENT_URI, values) != null)
                    result = true;
            } else
                result = true;
        } catch (Exception ex) {
            Log.e("PLACES", "Adding " + name + " failed.");
        }

        // If we haven't yet reached our prefetching limit, and we're either
        // on WiFi or don't have a WiFi-only prefetching restriction, and we
        // either don't have low batter or don't have a low battery prefetching
        // restriction, then prefetch the details for this newly added place.
        if ((prefetchCount < PlacesConstants.PREFETCH_LIMIT) &&
                (!PlacesConstants.PREFETCH_ON_WIFI_ONLY || !mobileData) &&
                (!PlacesConstants.DISABLE_PREFETCH_ON_LOW_BATTERY || !lowBattery)) {
            prefetchCount++;

            // Start the PlaceDetailsUpdateService to prefetch the details for this place.
            // As we're prefetching, don't force the refresh if we already have data.
            Intent updateServiceIntent = new Intent(this, PlaceDetailsUpdateService.class);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_REFERENCE, reference);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_ID, id);
            updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, false);
            startService(updateServiceIntent);
        }

        return result;
    }

    /**
     * Remove stale place detail records unless we've set the persistent cache flag to true.
     * This is typically the case where a place has actually been viewed rather than prefetched.
     *
     * @param location Location
     * @param radius   Radius
     */
    protected void removeOldLocations(Location location, int radius) {
        // Stale Detail Pages
        long minTime = System.currentTimeMillis() - PlacesConstants.MAX_DETAILS_UPDATE_LATENCY;
        String where = PlaceDetailsContentProvider.KEY_LAST_UPDATE_TIME + " < " + minTime + " AND " +
                PlaceDetailsContentProvider.KEY_FORCE_CACHE + " = 0";
        contentResolver.delete(PlaceDetailsContentProvider.CONTENT_URI, where, null);
        Log.d(TAG, "removeOldLocations");
    }

}