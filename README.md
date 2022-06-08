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

## Usage summary

- [Initial setup](#initial-setup)
- [Enable what3words features in an existing Google maps app using W3WGoogleMapsWrapper](#enable-what3words-features-in-an-existing-google-maps-app-using-W3WGoogleMapsWrapper)
- [Enable what3words features in an existing Mapbox maps app using W3WMapBoxWrapper](#enable-what3words-features-in-an-existing-mapbox-maps-app-using-W3WMapBoxWrapper)
- [General map wrapper functions](#general-map-wrapper-functions)
- [Enable what3words features in an new Google maps app using W3WGoogleMapFragment](#enable-what3words-features-in-an-new-google-maps-app-using-w3wgooglemapfragment)
- [Enable what3words features in an new Mapbox maps app using W3WMapboxMapFragment](#enable-what3words-features-in-an-new-mapbox-maps-app-using-w3wmapboxmapfragment)
- [General map fragment functions](#general-map-fragment-functions)

## Initial setup

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

## Enable what3words features in an existing Google maps app using W3WGoogleMapsWrapper

To use Google Maps on your app follow the quick start tutorial on Google developer portal here: https://developers.google.com/maps/documentation/android-sdk/start  
  
After a succesful Google maps run, you can start using our GoogleMapsWrapper, using the following steps:
  

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

    override fun onMapReady(map: GoogleMap) {
        val apiWrapper = What3WordsV3("YOUR_API_KEY_HERE", this)
        val googleMapsWrapper = W3WGoogleMapsWrapper(
            this,
            map,
            apiWrapper
        )

	//example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        googleMapsWrapper.addMarkerAtWords(
            "filled.count.soap",
            markerColor.BLUE,
            { marker ->
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(marker.coordinates.lat, marker.coordinates.lng))
                    .zoom(19f)
                    .build()
                p0.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }, { error ->
                Log.e("MainActivity", "${error.message}")
            }
        )

        //click event on existing w3w markers on the map.
        w3wMapsWrapper.onMarkerClicked { marker ->
            Log.i("MainActivity", "clicked: ${marker.words}")
        }

        //REQUIRED
        map.setOnCameraIdleListener {
            //existing code here...

            //needed to draw the 3x3m grid on the map
            googleMapsWrapper.updateMap()
        }

        //REQUIRED
        map.setOnCameraMoveListener {
            //existing code here...

            //needed to draw the 3x3m grid on the map
            googleMapsWrapper.updateMove()
        }

        map.setOnMapClickListener { latLng ->
            //existing code here...

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

## Enable what3words features in an existing Mapbox maps app using W3WMapBoxWrapper

To use Mapbox Maps on your app follow the quick start tutorial on Mapbox developer portal here: https://docs.mapbox.com/android/navigation/guides/get-started/install/

After a succesful Mapbox map run, you can start using our MapboxWrapper, using the following steps:

activity_main.xml
```Kotlin
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.mapbox.maps.MapView
        android:id="@+id/mapView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Kotlin
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        setContentView(binding.root)

        val wrapper = What3WordsV3("YOUR_API_KEY_HERE", this)
        val mapboxWrapper = W3WMapBoxWrapper(
            this,
            binding.mapView.getMapboxMap(),
            wrapper,
        ).setLanguage("en")
 
        //example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        mapboxWrapper.addMarkerAtWords(
            "filled.count.soap",
            markerColor.BLUE,
            { marker ->
                
            }, { error ->
                Log.e("MainActivity", "${error.message}")
            }
        )

        //click even on existing w3w added markers on the map.
        mapboxWrapper.onMarkerClicked {
            Log.i("MainActivity", "clicked: ${it.words}")
        }

        //REQUIRED
        binding.mapView.getMapboxMap().addOnMapIdleListener {
            //existing code here...

            //needed to draw the 3x3m grid on the map
            mapboxWrapper.updateMap()
        }

        //REQUIRED
        binding.mapView.getMapboxMap().addOnCameraChangeListener {
            //existing code here...

            //needed to draw the 3x3m grid on the map
            mapboxWrapper.updateMove()
        }

        binding.mapView.getMapboxMap().addOnMapClickListener { latLng ->
            //existing code here...

            //example of how to select a 3x3m w3w square using lat/lng
            val coordinates = Coordinates(
                latLng.latitude(),
                latLng.longitude()
            )
            mapboxWrapper.selectAtCoordinates(coordinates)
            true
        }
    }
}
```

If you run our Enterprise Suite API Server yourself, you may specify the URL to your own server like so:

```Kotlin
val wrapper = What3WordsV3("YOUR_API_KEY_HERE","https://api.yourserver.com", this)
```

## General map wrapper functions:

| Name - Summary | Example |
|---|---|
|**setLanguage**, set the language of three word address that the onSuccess callback should return. Parameter should be a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).|```setLanguage("en")```|
|**gridEnabled**, enable grid overlay over map with all 3mx3m squares on the visible map bounds, enabled by default.|```gridEnabled(true)```|
|**onMarkerClicked**, a callback for when an existing marker on the map is clicked.|```onMarkerClicked { marker -> }```|
|**addMarkerAtSuggestion**, add a suggestion to the map. This method will add a marker/square to the map after getting the Suggestion from our W3WAutosuggestEditText allowing easy integration between both components autosuggest and maps.|```addMarkerAtSuggestion(suggestion, markerColor.RED, { marker -> }, { error -> })```<br>or add multiple suggestions to the map: <br>```addMarkerAtSuggestion(suggestions, markerColor.RED, { markers -> }, { error -> })```|
|**removeMarkerAtSuggestion**, remove Suggestion from the map if exists.|```removeMarkerAtSuggestion(suggestion)```<br>or remove multiple suggestions: <br>```removeMarkerAtSuggestion(suggestions)```|
|**selectAtSuggestion**, set Suggestion as the selected marker on the map, it can only have one selected marker at the time.|```selectAtSuggestion(suggestion, { selectedmarker -> }, { error -> })```|
|**addMarkerAtCoordinates**, add marker at coordinates to the map. This method will add a marker/square to the map based on the latitude and the longitude provided.|```addMarkerAtCoordinates(Coordinates(49.180803, -8.001330), { marker -> }, { error -> })```<br>or add multiple Coordinates to the map: <br>```addMarkerAtCoordinates(listOf(Coordinates(49.180803, -8.001330), Coordinates(50.180803, -8.001330)), { markers -> }, { error -> }) ```
|**selectAtCoordinates**, set coordinates as selected marker on the map, it can only have one selected marker at the time.|```selectAtCoordinates(Coordinates(50.180803, -8.001330), { selectedMarker -> }, { error -> }```|
|**findMarkerByCoordinates**, find marker (returns null if no marker is added on the specified coordinates) added to map|```val marker = findMarkerByCoordinates(Coordinates(50.180803, -8.001330))```|
|**removeMarkerAtCoordinates**, remove marker at coordinates from the map.|```removeMarkerAtCoordinates(Coordinates(50.180803, -8.001330)) ```<br>or remove multiple markers at coordinates from the map: <br>```removeMarkerAtCoordinates(listOf(Coordinates(49.180803, -8.001330), Coordinates(50.180803, -8.001330)) ```|
|**addMarkerAtWords**, add a three word address to the map. This method will add a marker/square to the map if the works are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, onError will be called returning APIResponse.What3WordsError.BAD_WORDS.|```addMarkerAtWords("filled.count.soap"), markerColor.RED, { marker -> }, { error -> })```<br>or add multiple 3 word addresses to the map: <br>```addMarkerAtWords(listOf("filled.count.soap", "index.home.raft"), markerColor.RED, { markers -> }, { error -> })```|
|**selectAtWords**, set words as selected marker on the map, it can only have one selected marker at the time.|```selectAtWords("filled.count.soap", { selectedMarker -> }, { error-> })```|
|**removeMarkerAtWords**, remove marker at three word address from the map.|```removeMarkerAtWords("filled.count.soap")```<br>or remove multiple markers at three word adresses from the map: <br>```removeMarkerAtWords(listOf("filled.count.soap", "index.home.raft")) ```|
|**removeAllMarkers**, remove all markers added to the map |```removeAllMarkers()```|
|**getAllMarkers**, Gets all added markers from the map | ```val markers = getAllMarkers()```|
|**unselect**, remove selected marker from the map.|```unselect()```|
|**updateMap**, this method should be called on GoogleMap.setOnCameraIdleListener or MapboxMap.addOnMapIdleListener, this will allow to refresh the grid bounds and draw the grid (if enabled) on camera idle.|```updateMap()```<br>*mandatory if gridEnabled  is set to true (default)* |
|**updateMove**, This method should be called on GoogleMap.setOnCameraMoveListener or MapboxMap.addOnCameraChangeListener, this will allow to swap from markers to squares and show/hide grid when zoom goes higher or lower than the zoom level threshold (can differ per map provider).|```updateMove()```<br>*mandatory if gridEnabled is set to true (default)* |
 
 
## Enable what3words features in an new Google maps app using W3WGoogleMapFragment

Since you are creating a new app you can always opt to use our W3WGoogleMapFragment, the main advantage is that all the required events to draw the grid are done under the hood, resulting in less boilerplate code and still have access to the Google Map to apply normal customization (i.e mapTypes, etc.)

To use Google Maps on your app follow the quick start tutorial on Google developer portal here: https://developers.google.com/maps/documentation/android-sdk/start  
  
After a succesful Google maps run, you can start using our W3WGoogleMapFragment, using the following steps:
  

activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map"
    android:name="com.what3words.components.maps.views.W3WGoogleMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Kotlin
```Kotlin
class MainActivity : AppCompatActivity(), W3WGoogleMapFragment.OnFragmentReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as W3WGoogleMapFragment
        mapFragment.apiKey("YOUR_API_KEY_HERE", this)
    }

    override fun onFragmentReady(fragment: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        fragment.setLanguage("en")

      //example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        fragment.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            W3WZoomOption.CENTER_AND_ZOOM,
            { marker ->
                Log.i(
                    "MainActivity",
                    "added ${marker.words} at ${marker.coordinates.lat}, ${marker.coordinates.lng}"
                )
            }, { error ->
               	Log.e(
               	    "MainActivity",
               	    "${error.key}, ${error.message}"
               	)
            }
        )

        //if you want to access the google map instance inside W3WGoogleMapFragment do the following
        (fragment as? W3WGoogleMapFragment)?.getMap()?.let {
            it.mapType = MAP_TYPE_NORMAL
        }
    }

```

## Enable what3words features in an new Mapbox maps app using W3WMapboxMapFragment

Since you are creating a new app you can always opt to use our W3WMapboxMapFragment, the main advantage is that all the required events to draw the grid are done under the hood, resulting in less boilerplate code and still have access to the Mapbox Map to apply normal customization (i.e map types, etc.)

To use Mapbox Maps on your app follow the quick start tutorial on Mapbox developer portal here: https://docs.mapbox.com/android/navigation/guides/get-started/install/

After a succesful Mapbox map run, you can start using our MapboxWrapper, using the following steps:

activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map"
    android:name="com.what3words.components.maps.views.W3WMapboxMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Kotlin
```Kotlin
class MainActivity : AppCompatActivity() , W3WMapboxMapFragment.OnFragmentReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as W3WMapboxMapFragment
        mapFragment.apiKey("YOUR_API_KEY_HERE", this)
    }

    override fun onFragmentReady(fragment: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        fragment.setLanguage("en")

      //example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        fragment.addMarkerAtWords(
            "filled.count.soap",
            W3WMarkerColor.BLUE,
            W3WZoomOption.CENTER_AND_ZOOM,
            { marker ->
                Log.i(
                    "MainActivity",
                    "added ${marker.words} at ${marker.coordinates.lat}, ${marker.coordinates.lng}"
                )
            }, { error ->
               	Log.e(
               	    "MainActivity",
               	    "${error.key}, ${error.message}"
               	)
            }
        )

        //if you want to access the mapbox map instance inside W3WMapboxMapFragment do the following
        (fragment as? W3WMapboxMapFragment)?.getMap()?.let {
            it.loadStyleUri(Style.MAPBOX_STREETS)
        }
    }
}
```


## General map fragment functions:

| Name - Summary | Example |
|---|---|
|**setLanguage**, set the language of three word address that the onSuccess callback should return. Parameter should be a supported 3 word address language as an ISO 639-1 2 letter code. Defaults to en (English).|```setLanguage("en")```|
|**onMarkerClicked**, a callback for when an existing marker on the map is clicked.|```onMarkerClicked { marker -> }```|
|**addMarkerAtSuggestion**, add a suggestion to the map. This method will add a marker/square to the map after getting the Suggestion from our W3WAutosuggestEditText allowing easy integration between both components autosuggest and maps.|```addMarkerAtSuggestion(suggestion, markerColor.RED, { marker -> }, { error -> })```<br>or add multiple suggestions to the map: <br>```addMarkerAtSuggestion(suggestions, markerColor.RED, { markers -> }, { error -> })```|
|**removeMarkerAtSuggestion**, remove Suggestion from the map if exists.|```removeMarkerAtSuggestion(suggestion)```<br>or remove multiple suggestions: <br>```removeMarkerAtSuggestion(suggestions)```|
|**selectAtSuggestion**, set Suggestion as the selected marker on the map, it can only have one selected marker at the time.|```selectAtSuggestion(suggestion, { selectedmarker -> }, { error -> })```|
|**addMarkerAtCoordinates**, add marker at coordinates to the map. This method will add a marker/square to the map based on the latitude and the longitude provided.|```addMarkerAtCoordinates(Coordinates(49.180803, -8.001330), { marker -> }, { error -> })```<br>or add multiple Coordinates to the map: <br>```addMarkerAtCoordinates(listOf(Coordinates(49.180803, -8.001330), Coordinates(50.180803, -8.001330)), { markers -> }, { error -> }) ```
|**selectAtCoordinates**, set coordinates as selected marker on the map, it can only have one selected marker at the time.|```selectAtCoordinates(Coordinates(50.180803, -8.001330), { selectedMarker -> }, { error -> }```|
|**findMarkerByCoordinates**, find marker (returns null if no marker is added on the specified coordinates) added to map|```val marker = findMarkerByCoordinates(Coordinates(50.180803, -8.001330))```|
|**removeMarkerAtCoordinates**, remove marker at coordinates from the map.|```removeMarkerAtCoordinates(Coordinates(50.180803, -8.001330)) ```<br>or remove multiple markers at coordinates from the map: <br>```removeMarkerAtCoordinates(listOf(Coordinates(49.180803, -8.001330), Coordinates(50.180803, -8.001330)) ```|
|**addMarkerAtWords**, add a three word address to the map. This method will add a marker/square to the map if the works are a valid three word address, e.g., filled.count.soap. If it's not a valid three word address, onError will be called returning APIResponse.What3WordsError.BAD_WORDS.|```addMarkerAtWords("filled.count.soap"), markerColor.RED, { marker -> }, { error -> })```<br>or add multiple 3 word addresses to the map: <br>```addMarkerAtWords(listOf("filled.count.soap", "index.home.raft"), markerColor.RED, { markers -> }, { error -> })```|
|**selectAtWords**, set words as selected marker on the map, it can only have one selected marker at the time.|```selectAtWords("filled.count.soap", { selectedMarker -> }, { error-> })```|
|**removeMarkerAtWords**, remove marker at three word address from the map.|```removeMarkerAtWords("filled.count.soap")```<br>or remove multiple markers at three word adresses from the map: <br>```removeMarkerAtWords(listOf("filled.count.soap", "index.home.raft")) ```|
|**removeAllMarkers**, remove all markers added to the map |```removeAllMarkers()```|
|**getAllMarkers**, Gets all added markers from the map | ```val markers = getAllMarkers()```|
|**unselect**, remove selected marker from the map.|```unselect()```|