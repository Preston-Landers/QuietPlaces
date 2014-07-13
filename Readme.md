
# Welcome to QuietPlaces #

This is the source code repository for QuietPlaces, an Android app that automatically
silences your phone when you enter quiet places that you have defined. When you leave
the quiet zone your ringer is automatically re-enabled. Quiet Place zones are defined as
circles centered around a geographic point.

You can create, remove or adjust your quiet places at any time using an interactive map,
and new quiet places can be automatically placed based on categories obtained from
the Google Places API.

Warning: this app should be considered experimental. Do not use this app on your main
phone if having your phone unexpectedly silenced as you move around would be a problem.

## About QuietPlaces ##

This is a project for a Mobile Computing class at UT Austin. The goal is to combine the
Google Maps and Places APIs to enable intelligent control over the phone ringer by sensing
 when the user has entered places categorized as silent zones, such as hospitals or museums.

## Download Now! ##
The app is now available on the [Google Play Store](https://play.google.com/store/apps/details?id=edu.utexas.quietplaces)!

## Authors ##

Preston Landers _planders at utexas dot edu_

Abe Arredondo _abe_arredondo at utexas dot edu_

Thanks to:

Dr. Christine Julien


# Current Status #

See the section below for building the app from source code.

The app is has basic functionality working. It handles manual placement and adjustment of
Quiet Places as well as automatic place suggestion using the Google Places API.  The
automatic placement and sizing could use some additional refinement.

We also have a mock location testing companion app, which is available here:
[QuietPlacesMockLocations]. See below for more information about testing.

## Usage Summary ##

The app has a built-in help screen in the navigation drawer. Please see that for more information.

This app uses a [navigation drawer](https://developer.android.com/design/patterns/navigation-drawer.html) interface.
There is a home screen with a ringer control switch. The main interface of the app is the Map tab.
From this tab you can manually place quiet places on the map by pressing the Add button
then pressing the spot to add. You can move places by holding the marker down then dragging it.
You can resize, rename, or delete the place by pressing on the marker to select it.
Select marker again to un-select it.

There is a Follow checkbox to control whether the camera follows your current position.
The standard Google map controls - 'my location' in the upper right, and zoom in and out on the bottom right.

There is also a History panel with a log of system events such as entering or leaving a quiet place.

You can enable automatic placement of Quiet Places by going to the Settings screen and selecting categories.
These automatic places are periodically updated on the map; it may take up to 1 minute.  You can convert
a suggested place to a permanently saved one by selecting it and then modifying it somehow (move or resize
for example.)

There are also settings to control whether or not we use Location Services.
If this is disabled, the app won't automatically suggest places and won't update your position on the map.
However, previously defined Quiet Places may still be activated.

There's also settings to control whether we actually silence the ringer, and whether to use vibrate or silence.

## Future Features / TODO List  ##

* Use better looking map marker drawables, especially for auto-places
* Generate more interesting names for manually added places (e.g. find an address or name)
* Ability to resize quiet places with scale gestures
    * Not critical because we have buttons to resize the selected place but would be cool.
* Better placement of auto-QPs. E.g. don't center the circle on the street corner
* Better sizing of auto-QPs, at least some basic heuristics
* Allow an individual Quiet Place to be temporarily disabled w/o deleting it.
* Put a confirmation dialog on delete place, and clear history?
* Use the 'user activity detection' features to determine if the user is driving
  and suspend enforcement? (as an option)

## Known Bugs ##

* There is a hard limit of 100 active geofences imposed by Location Services.
  This isn't handled very gracefully yet. We should self-limit the number we
  try to create.  And also put in a preference to allow the maximum number.
* If you are currently inside a geofence, and then move the fence away from you with a drag,
  it doesn't register as leaving the geofence since it got removed and then
  readded in the other spot.
    * Likewise, if you create a new QP that you are currently inside, it doesn't trigger the silence.
* When you change the selected categories, the auto places don't update until
  you move a minimum distance (100 m), or 1 hour has passed. Even if you
  are actively moving it can take up to a minute to register changes to the automatic place categories.
* Current selection (of a Quiet Place) is lost when changing device orientation.


# Development Notes #

## Architecture Overview ##

The app maintains a database with two types of map markers: manually placed and automatically
placed. When we place a map marker we create an associated [Geofence definition in Google Play Location Services]
(https://developer.android.com/training/location/geofencing.html).

The geofence sends the app a notification when the user enters or exits a circularly defined
geographic region (a point plus a radius.)  The MainActivity registers a broadcast receiver for this
geofence transition event and passes the event to the associated QuietPlaceMapMarker (QPMM, see below) object.
The QPMM determines if the ringer should be silenced or unsilenced. For instance, we should not unsilence if we're still
inside another overlapping Quiet Place. These geofence events are processed even if the app is
in the background.

The MainActivity also maintains another listener for general purpose location updates. These are currently
used for two purposes:

* Following the user's currently location on the map (if the option is checked)
* Periodically querying the Google Places API with the current location to look for nearby
  places that match our preferences.

The Places API query happens anywhere from every 30 seconds to up to an hour, depending how far the user has moved
since the last query. The response may also be cached locally by the app. The API query happens in a background
Service outside the main UI thread. Further processing of the results is done inside an AsyncTask from
the main activity to keep the application responsive.

The AsyncTask scans the API results from the PlacesUpdateService and finds new Quiet Places to add to the map,
and prunes out QPs that no longer match our criteria or are too far away.

## App Permissions ##

The app requires the following Android system permissions:

* Location data (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION)
* Network access for the Places API  (INTERNET)
    * Check whether we're connected to the network (ACCESS_NETWORK_STATE)
* Cache map data on the device for better performance (WRITE_EXTERNAL_STORAGE)

## Tools and APIs used ##

We used the following APIs and libraries to create this app:

* [Google Maps for Android V2](https://developers.google.com/maps/documentation/android/)
* [Google Places API](https://developers.google.com/places/)
* [JodaTime library](http://www.joda.org/joda-time/)
* Some code was adapted from a project called
  [Android Location Best Practices](https://code.google.com/p/android-protips-location/) in heavily modified form.
  See [this article about LBP.](http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html)

Note that if you wish to build this app from source you need to obtain your own Google API keys as described below.


## Data Model ##

QuietPlace objects ([POJOs](http://en.wikipedia.org/wiki/Plain_Old_Java_Object)) represent
our basic database record. Each geofence zone, whether manually added or automatically
suggested, gets put in the database as a QuietPlace.

The QuietPlaceMapMarker (QPMM) is an object which ties together the QuietPlace database record
plus the Google Map marker, the geofence status, and associated data and methods, such as to move
or resize it.

QPMapFragment extends the basic Google Map view and adds our custom map behaviors
and manages the collection of QPMMs. Custom map controls are implemented as an overlay
on top of the Google MapView.

The Quiet Place definitions are stored in a [SQLite database](http://developer.android.com/training/basics/data-storage/databases.html)
 local to the phone and accessed through a standard [ContentProvider interface](http://developer.android.com/guide/topics/providers/content-providers.html).

The same applies to other data objects managed by the app - history events and Google Place API records that
are cached in the local database.

## Software Engineering Challenges ##

This section describes some of the mobile computing and software engineering challenges
encountered developing this project. These include the difficulty of readily testing an heavily
location-based app, and prevent excessive battery drain by the app.

### Testing location-centric apps is hard. ###

One approach is to feed mock location data to the app with the assistance of another app, the mock
location provider. This has the benefit of working inside the emulator and allows testing a sequence
of location events without physically moving the device.

We have a [mock location provider app][QuietPlacesMockLocations] based on
the [sample code provided by Google][Location Testing].

Currently this app is loaded with a tour of downtown Austin and UT campus. The information was
generated by drawing paths in Google Earth, then exporting the data and running it through a
Python program to put it in a format suitable for the mock location app. For more information
please see the [project page for QuietPlacesMockLocations][QuietPlacesMockLocations].

[QuietPlacesMockLocations]: https://bitbucket.org/planders/quietplacesmocklocations
[Location Testing]: http://developer.android.com/training/location/location-testing.html

### Accuracy and energy usage ###

The app makes an attempt to use as little energy as possible. The more 'Quiet Places' (geofences) that
are active, the more energy the app will use.  It uses the most energy when the map is visible on screen
due to frequent location updates. When the app is in the background it updates its location
much less often.

General information about usage limits in the Google Places API and rate limiting strategies:

https://developers.google.com/maps/documentation/business/articles/usage_limits


# Building from Source #

This is a Gradle based Android project which is buildable from the command line, or from
Gradle-aware IDEs such as IntelliJ IDEA 13 or Android Studio. It should work with
Eclipse but I haven't tried that.

*Important*: In order to build and run the app from source, you must create a new private.xml file
and put in a Google API key as described below. The app won't compile unless you take this extra
step of creating a file and inserting a new API key that you got from Google.

Note that you can't have the Store version of the app installed at the same time as your
custom compiled version unless you change the package name of your version. IntelliJ can refactor this
for you in one step. If you don't change the Android package name, you will have to uninstall the store
version and lose any saved Quiet Place definitions.

## Prerequisites to Build ##

* JDK 1.6
* Android SDK installed:
    * Android SDK Platform/Build Tools
    * Android API 20
    * Android Support Library
    * Android Support Repository
    * Google Play Services
    * Google Repository
* Either: a Java IDE that includes Gradle such as IntelliJ IDEA 13 or Android Studio.
    * OR a Gradle install if you wish to build from the command line
* Obtain a new pair of API keys as described below.


## Add the API Key in res/values/private.xml ##

In order to build from source you must provide an XML file containing the two API keys needed
to access Google APIs.

For security reasons these keys are not checked into version control. If you wish to build your own
version of this app you will need to register a new project in the Google API console and obtain
two new API keys.

First obtain an Android client key for the Maps API following the instructions here:

[Google Maps Android instructions](https://developers.google.com/maps/documentation/android/start#creating_an_api_project)

Next obtain a "browser" key for the Places API. This can be done in the Google project API console.
Create a browser key with no referrers.

Create the following file:

    QuietPlaces/src/main/res/values/private.xml

Contents should be like:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="google_public_api_key">API_KEY</string>
        <string name="google_browser_api_key">BROWSER_API_KEY</string>
    </resources>

Replace `API_KEY` with the Android key you got from the Public API Access area of the console.

Replace `BROWSER_API_KEY` wtih the browser key from the same area of the console.

The keys will look something like `AIzaSyBdVl-cTICSwYKrZ95SuvNw7dbMuDt1KG0` (this is not a valid key)


## Debug and Release builds ##

This section is only relevant if you are trying to build a public release version for the App Store.

Debug builds will be signed with your default debug keystore. To build a debug version with gradle run:

    gradle assembleDebug

Note that you may need to put your debug keystore's SHA1 signature into the Google API Console in the
public API access key for Android.

The official release build uses a private keystore that is not included in the version
control repository.  You must define the release keystore location and password in your
`gradle.properties` file:

    ~/.gradle/gradle.properties

Contents:

    QP_RELEASE_STORE_FILE=C:/Users/myName/Documents/Android/quietplaces_release.keystore
    QP_RELEASE_STORE_PASSWORD=YourPasswordHere
    QP_RELEASE_KEY_ALIAS=quietplaces_release
    QP_RELEASE_KEY_PASSWORD=YourPasswordHere

Now you should be able to create a signed release build with:

    gradle assembleRelease

# License

Unless otherwise noted, the code in this project is free software and is licensed by the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
See LICENSE.txt for details.


# Open Source Credits

Some icons were taken from [this icon set]
(http://www.iconarchive.com/show/sleek-xp-basic-icons-by-hopstarter.html)
under Creative Commons license.

