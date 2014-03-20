
# Welcome to QuietPlace #

This is the source code repository for QuietPlace, an Android app to automatically
silence your phone when you enter quiet places. You can create, remove and adjust your
quiet places at any time.

## About QuietPlace ##

This is a project for a Mobile Computing class at UT Austin. The goal is to combine the
Google Maps and Places APIs to enable intelligent control over the phone ringer by sensing
 when the user has entered a place categorized as a silent zone, such as hospitals or museums.


## Authors ##

Preston Landers _planders at utexas dot edu_

Abe Arredondo _abe_arredondo at utexas dot edu_

Thanks to:

Dr. Christine Julien

# Development Notes #

Currently we have a simple with a switch that (manually) controls the ringer.

We also display a map thought don't do anything with it currently.

## Current TODO List ##

* Create a SQLite database to store manually defined quiet zones.
* Draw quiet places on the map
* Discover/suggest quiet places automatically
* Implement geofenced ringer around quiet places

## Software Engineering Challenges ##

### Testing location-centric apps is hard. ###

One approach is to feed mock location data to the app with the assistance of another app, the mock
location provider. This has the benefit of working inside the emulator and allows testing a sequence
of location events without physically moving the device.

I have started a [QuietPlacesMockLocations][mock location provider app] from
the [Location Testing][sample code provided by Google].  Currently this has just a single
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
and put in a Google API key as described below.

Note: IntelliJ IDEA 13 keeps creating 'duplicate' libraries. I'm experiencing this but don't have a fix yet:

 http://stackoverflow.com/questions/20728492/how-to-avoid-mirroring-libraries-in-intellij-idea-using-gradle

## Add the API Key in res/values/private.xml ##

In order to build from source you must provide an XML file containing the Google Maps API key.
For security reasons this is not checked into version control. If you wish to build your own
version of this app you will need to register a new project in the Google API console and obtain
a new API key following the instructions here:

[Google Maps Android instructions](https://developers.google.com/maps/documentation/android/start#creating_an_api_project)

Create the following file:

    QuietPlaces/src/main/res/values/private.xml

Contents should be like:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="google_project_number">PROJECT_NUMBER</string>
        <string name="google_public_api_key">API_KEY</string>
    </resources>

Replace `PROJECT_NUMBER` with the project number from the Google API console, and the `API_KEY` from the
key you got from the Public API Access area of the console.

`API_KEY` will look something like `AIzaSyBdVl-cTICSwYKrZ95SuvNw7dbMuDt1KG0` (that's not a valid one)


## Debug and Release builds ##

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

# Open Source Credits

Icons from:
http://www.iconarchive.com/show/sleek-xp-basic-icons-by-hopstarter.html
