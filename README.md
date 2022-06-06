# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-map-components

An Android library to use the [what3words v3 grid and squares features on different map providers](https://developer.what3words.com/public-api/docs#grid-section).

TO INSERT GIF HERE

To obtain an API key, please visit [https://what3words.com/select-plan](https://what3words.com/select-plan) and sign up for an account.

## Installation

The artifact is available through [![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-components.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-components%22) - REPLACE WITH MAP

### Android minumum SDK support
[![Generic badge](https://img.shields.io/badge/minSdk-23-green.svg)](https://developer.android.com/about/versions/marshmallow/android-6.0/)

### Gradle

```
implementation 'com.what3words:w3w-android-map-components:1.0.0'
```

## Documentation

See the what3words public API [documentation](https://docs.what3words.com/api/v3/)

## Usage

### Enable what3words features in an existing Google maps app

To use Google Maps on your app follow the quick start tutorial on Google developer portal here: https://developers.google.com/maps/documentation/android-sdk/start  
  
  After a succesful Google maps run, you can start using our GoogleMapsWrapper, using the following steps:
  
AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.yourpackage.yourapp">

    <uses-permission android:name="android.permission.INTERNET" />
    ...
```

add this to build.gradle (app level)
```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

add this the following proguard rules
```
-keep class com.what3words.javawrapper.request.* { *; }
-keep class com.what3words.javawrapper.response.* { *; }
```

activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
...
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/map"
    android:name="com.google.android.gms.maps.SupportMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
Kotlin
```Kotlin
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
    }

    override fun onMapReady(p0: GoogleMap) {
        val apiWrapper = What3WordsV3("YOUR_API_KEY_HERE", this)
        val googleMapsWrapper = W3WGoogleMapsWrapper(
            this,
            p0,
            apiWrapper
        )

	//example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        googleMapsWrapper.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            { w3wMarker ->
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(w3wMarker.coordinates.lat, w3wMarker.coordinates.lng))
                    .zoom(19f)
                    .build()
                p0.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, { error ->
                //log error
            }
        )

        //click event on existing w3w markers on the map.
        w3wMapsWrapper.onMarkerClicked { w3wMarker ->
            Log.i("UsingMapWrapperActivity", "clicked: ${w3wMarker.words}")
        }

        //REQUIRED
        p0.setOnCameraIdleListener {
            //...

            //needed to draw the 3x3m grid on the map
            googleMapsWrapper.updateMap()
        }

        //REQUIRED
        p0.setOnCameraMoveListener {
            //...

            //needed to draw the 3x3m grid on the map
            googleMapsWrapper.updateMove()
        }

        p0.setOnMapClickListener { latLng ->
            //..

            //example of how to select a 3x3m w3w square using lat/lng
            val coordinates = Coordinates(
                latLng.latitude,
                latLng.longitude
            )
            googleMapsWrapper.selectAtCoordinates(coordinates)
        }
    }
}
```

If you run our Enterprise Suite API Server yourself, you may specify the URL to your own server like so:

```Kotlin
val wrapper = What3WordsV3("YOUR_API_KEY_HERE","https://api.yourserver.com", this)
```

## General map wrapper functions:

| Name | Summary | Example |
|---|---|----|
|setLanguage|Set the language of [SuggestionWithCoordinates.words] that onSuccess callbacks should return. Parameter should be a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).|```setLanguage("en")```|
|gridEnabled|Enable grid overlay over map with all 3mx3m squares on the visible map bounds, enabled by default.|```gridEnabled(true)```|
|onMarkerClicked|A callback for when an existing marker on the map is clicked.|```onMarkerClicked { w3wMarker -> }```|
|addMarkerAtSuggestion|Add a suggestion to the map. This method will add a marker/square to the map after getting the Suggestion from our W3WAutosuggestEditText allowing easy integration between both components autosuggest and maps.|```addMarkerAtSuggestion(suggestion, W3WMarkerColor.RED, {marker -> }, {error -> })```<br>or add multiple suggestions to the map: <br>```addMarkerAtSuggestion(suggestions, W3WMarkerColor.RED, {markers -> }, {error -> })```|
|removeMarkerAtSuggestion|Remove Suggestion from the map if exists.|```removeMarkerAtSuggestion(suggestion)```<br>or remove multiple suggestions: <br>```removeMarkerAtSuggestion(suggestions)```|
|selectAtSuggestion|Set Suggestion as the selected marker on the map, it can only have one selected marker at the time.|```selectAtSuggestion(suggestion, {w3wMarker -> }, {error -> })```|
|addMarkerAtCoordinates|Add marker at coordinates to the map. This method will add a marker/square to the map based on the latitude and the longitude provided.|```addMarkerAtCoordinates(Coordinates(49.180803, -8.001330), { w3wMarker -> }, { error -> })```<br>or add multiple Coordinates to the map: <br>```addMarkerAtCoordinates(listOf(Coordinates(49.180803, -8.001330), Coordinates(50.180803, -8.001330)), { w3wMarkers -> }, { error -> }) ```|