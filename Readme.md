
# Welcome to QuietPlaces #

This is the source code repository for QuietPlaces, an Android app that automatically
silences your phone when you enter quiet places that you have defined. When you leave
the quiet zone your ringer is automatically re-enabled. Quiet Place zones are defined as
circles centered around a geographic point.

You can create, remove or adjust your quiet places at any time using an interactive map,
and new quiet places can be automatically suggested based on categories obtained from
the Google Places API.

## About QuietPlace ##

This is a project for a Mobile Computing class at UT Austin. The goal is to combine the
Google Maps and Places APIs to enable intelligent control over the phone ringer by sensing
 when the user has entered places categorized as silent zones, such as hospitals or museums.


## Authors ##

Preston Landers _planders at utexas dot edu_

Abe Arredondo _abe_arredondo at utexas dot edu_

Thanks to:

Dr. Christine Julien

# Current Status #

See the section below for building the app from source code.

The app is largely functional. It handles manual placement and adjustment of Quiet Places,
as well as automatic place suggestion from the Google Places API.  The automatic placement
and sizing could use some refinement.

The app has a built-in help screen in the navigation drawer. Please see that for basic usage information.

We also have a mock location testing companion app, which is available here:
[QuietPlacesMockLocations]. See below for more information about testing.

## Future Features / TODO List  ##

* Make a way to disable the geofencing ringer control from the home screen
* Use better looking map marker drawables, especially for auto-places
* Generate more interesting names for manually added places (e.g. find an address or name)
* Ability to resize quiet places with scale gestures
  * Not critical because we have buttons to resize the selected place.
* Better placement of auto-QPs. E.g. don't center the circle on the street corner
* Better sizing of auto-QPs, at least some basic heuristics
* Allow a Quiet Place to be temporarily disabled w/o deleting it.
* Put a confirmation dialog on delete place, and clear history?

## Known Bugs ##

* If you are currently inside a geofence, and then move the fence away from you with a drag,
  it doesn't register as leaving the fence since it got removed and then
  readded in the other spot.
* If you create a new QP that you are currently inside, it doesn't trigger the silence.
* If the app has not received location updates in a while, we may miss some geofence
  transitions and may need to do a manual check.
* When you change the selected categories, the auto places don't update until
  you move a minimum distance (100 m), or 1 hour has passed.
* Ringer switch on home screen can get out of sync with actual ringer status.
  * Need to register a listener for when the ringer changes, and update our switch from that
* Current selection (of a Quiet Place) is lost when changing device orientation
* There is a hard limit of 100 active geofences imposed by Location Services.
  This isn't really handled gracefully.

# Development Notes #

## Application Overview ##

The app maintains a database with two types of map markers: manually placed and automatically
placed. When we place a map marker we set up an associated [Geofence with Google Play Location Services]
(https://developer.android.com/training/location/geofencing.html).

The geofence sends the app a notification when the user enters or exits a circularly defined
geographic region (a point plus a radius.)  The MainActivity registers a broadcast receiver for this
geofence event and passes the event to the QuietPlaceMapMarker (QPMM, see below). The QPMM determines
if the ringer should be silenced or unsilenced. For instance, we should not unsilence if we're still
inside another overlapping Quiet Place. These geofence events are processed even if the app is
in the background.

The MainActivity also maintains another listener for general purpose location updates. These are currently
used for two purposes:

* Following the user's currently location on the map (if the option is checked)
* Periodically querying the Google Places API with the current location, to look for nearby
  places that match our preferences.

The Places API query happens anywhere from every 60 seconds to up to an hour, depending how far the user has moved
since the last query. The response may also be cached locally by the app. The query happens in a background
Service outside the main UI thread. Further processing of the results is done inside an AsyncTask from
the main activity.

It scans the API results for new Quiet Places to add to the map, and prunes out QPs that no longer match
our criteria or are too far away.

## Tools and APIs used ##

Note that if you wish to build this app from source you need to obtain your own Google API keys as described below.

* [Google Maps for Android V2](https://developers.google.com/maps/documentation/android/)
* [Google Places API](https://developers.google.com/places/)
* [JodaTime library](http://www.joda.org/joda-time/)

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


## Software Engineering Challenges ##

This section describes some of the mobile computing and software engineering challenges
encountered developing this project.

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

TODO...

https://developers.google.com/maps/documentation/business/articles/usage_limits


# Building from Source #

This is a Gradle based Android project which is buildable from the command line, or from
Gradle-aware IDEs such as IntelliJ IDEA 13 or Android Studio. It should work with
Eclipse but I haven't tried that.

*Important*: In order to build and run the app from source, you must create a new private.xml file
and put in a Google API key as described below. The app won't compile unless you take this extra
step of creating a file and inserting a new API key that you got from Google.

## Prerequisites to Build ##

* JDK 1.6
* Android SDK installed
  * Android SDK Platform/Build Tools
  * Android API 19
  * Android Support Library
  * Google Play Services
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
        <string name="google_project_number">PROJECT_NUMBER</string>
        <string name="google_public_api_key">API_KEY</string>
        <string name="google_browser_api_key">BROWSER_API_KEY</string>
    </resources>

Replace `PROJECT_NUMBER` with the project number from the Google API console, and the `API_KEY` from the
Android key you got from the Public API Access area of the console.

Replace `BROWSER_API_KEY` from the browser key from the same area of the console.

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

Some classes in this app were derived from the "Location Best Practices" project:

https://code.google.com/p/android-protips-location/
http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html

Some icons from:
http://www.iconarchive.com/show/sleek-xp-basic-icons-by-hopstarter.html

