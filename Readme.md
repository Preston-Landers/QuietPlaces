
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

# Development Notes #

## Current Status ##

See the section below for building the app from source code.

The app has basic functionality with user-defined quiet places (geofences). Automatic
suggestions of places based on the Places API is not implemented yet.

We also have a mock location testing companion app, which is available here:
[QuietPlacesMockLocations]

The Mock Location app will feed a series of fake location events to the location service,
if mock locations is enabled in the device's developer options menu.

## Current TODO List  ##

* Discover/suggest quiet places automatically from Places API
* Generate more interesting names for the places and allow name editing
* Ability to resize quiet places with scale gestures
  * Not critical because we have buttons to resize the selected place.
* Add a general purpose "active" button to the home screen - same as Use Location in settings?
* Use Async Task to load quiet places from database! Avoid long pauses in startup
* Only refresh the Places list if we stray far enough from the location of the last check
  * I thought the LBP code did this..?

## Known Bugs ##

* Current selection (of a Quiet Place) is lost when changing device orientation
* If you are currently inside a geofence, and then move the fence away from you,
  it doesn't register as leaving the fence since it got removed and then
  readded in the other spot.
* If you create a new QP that you are currently inside, it doesn't trigger the silence.
  * Should it?
* Put a confirmation dialog on delete place, and clear history?
* If the app has not received location updates in a while, we may miss some geofence
  transitions and may need to do a manual check.
* onResume is called twice during activity startup (with an onPause in between)
   * Why? This does happen in the LBP project too
   * This generates extra Places HTTP requests.

### other things to check ###

https://code.google.com/p/android-protips-location/issues/detail?id=11


## Software Engineering Challenges ##

This section describes some of the mobile computing and software engineering challenges
encountered developing this project.

### Testing location-centric apps is hard. ###

One approach is to feed mock location data to the app with the assistance of another app, the mock
location provider. This has the benefit of working inside the emulator and allows testing a sequence
of location events without physically moving the device.

I have started a [mock location provider app][QuietPlacesMockLocations] from
the [sample code provided by Google][Location Testing].  Currently this has just a single
hardcoded location provided by the sample code. We can customize that app to put in our own
sequence of events, or even better, make the app retrieve the sequence from a website that we
set up. This allows us to change the mock locations without updating the app.

[QuietPlacesMockLocations]: https://bitbucket.org/planders/quietplacesmocklocations
[Location Testing]: http://developer.android.com/training/location/location-testing.html

### Accuracy and energy usage ###

TODO...

# Building from Source #

This is a Gradle based project which is buildable from the command line, or from
Gradle-aware IDEs such as IntelliJ IDEA 13 or Android Studio. It should work with
Eclipse but I haven't tried that.

*Important*: In order to build and run the app from source, you must create a new private.xml file
and put in a Google API key as described below. The app won't compile unless you take this extra
step of creating a file and inserting a new API key that you got from Google.


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
[GNU General Public License (GPL) 2.0](http://www.gnu.org/licenses/gpl-2.0.txt).
See LICENSE.txt for details.


# Open Source Credits

Portions of this app were derived from the "Location Best Practices" project:

https://code.google.com/p/android-protips-location/
http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html

Some icons from:
http://www.iconarchive.com/show/sleek-xp-basic-icons-by-hopstarter.html

