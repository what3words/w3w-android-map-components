# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-map-components

[![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-map-components.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-map-components%22)

### Android minimum SDK support

[![Generic badge](https://img.shields.io/badge/minSdk-23-green.svg)](https://developer.android.com/about/versions/marshmallow/android-6.0/)


## Introduction

The what3words Map Component provides a straightforward way to add what3words to a Google or Mapbox
map and display features such as the what3words grid and what3words markers with what3words address.

If adding what3words to an existing map then we provide map
wrappers: [W3WGoogleMapsWrapper](https://github.com/what3words/w3w-android-map-components/tree/readme#enable-what3words-features-in-an-existing-google-maps-app-using-W3WGoogleMapsWrapper)
and [W3WMapBoxWrapper](https://github.com/what3words/w3w-android-map-components/tree/readme#enable-what3words-features-in-an-existing-mapbox-maps-app-using-W3WMapBoxWrapper)
.

If creating a new map using our component then we provide map
fragments: [W3WGoogleMapFragment](https://github.com/what3words/w3w-android-map-components/tree/readme#enable-what3words-features-in-an-new-google-maps-app-using-w3wgooglemapfragment)
and [W3WMapboxMapFragment](https://github.com/what3words/w3w-android-map-components/tree/readme#enable-what3words-features-in-an-new-mapbox-maps-app-using-w3wmapboxmapfragment)
.

<img src="https://github.com/what3words/w3w-android-map-components/blob/readme/assets/google-maps-sample.gif" width=30% height=30%>

To obtain an API key, please
visit [https://what3words.com/select-plan](https://what3words.com/select-plan) and sign up for an
account.

### Gradle

```
implementation 'com.what3words:w3w-android-map-components:$latest'
```

## Sample using MapWrapper and MapFragment in compose and xml for MapBox of the w3w-android-map-components library

[mapbox-sample](https://github.com/what3words/w3w-android-samples/tree/main/mapbox-sample)

## Sample using MapWrapper and MapFragment in compose and xml for Google Map of the w3w-android-map-components library

[maps-googlemaps-sample](https://github.com/what3words/w3w-android-samples/tree/main/maps-googlemaps-sample)



## MapWrapper

Google Map

To use Google Maps on your app, follow the quick start tutorial on the Google developer portal
here: https://developers.google.com/maps/documentation/android-sdk/start

After a successful Google Maps run, you can start using our GoogleMapsWrapper following these steps:

```Kotlin
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    ...

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
            googleMapsWrapper.selectAtCoordinates(latLng.latitude, latLng.longitude)
        }
    }
}
```

MapBox

To use Mapbox Maps on your app, follow the quick start tutorial on the Mapbox developer portal
here: https://docs.mapbox.com/android/navigation/guides/get-started/install/

After a successful Mapbox map run, you can start using our MapboxWrapper following these steps:

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
            mapboxWrapper.selectAtCoordinates(latLng.latitude(), latLng.longitude())
            true
        }
    }
}
```

If you run our Enterprise Suite API Server yourself, you may specify the URL to your own server like
so:

```Kotlin
val wrapper = What3WordsV3("YOUR_API_KEY_HERE", "https://api.yourserver.com", this)
```

## General map wrapper functions:

| Name - Summary | Example |
|---|---|
|**setLanguage**, set the language of what3words address that the onSuccess callback should return. The parameter should be a supported what3words address language as an [ISO 639-1 2 letter code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes). Defaults to en (English).|```setLanguage("en")```|
|**gridEnabled**, enable grid overlay over the map with all 3mx3m squares on the visible map bounds, enabled by default.|```gridEnabled(true)```|
|**onMarkerClicked**, a callback for when an existing marker on the map is clicked.|```onMarkerClicked { marker -> }```|
|**addMarkerAtSuggestion**, add a suggestion to the map. This method will add a marker/square to the map after getting the Suggestion from our [W3WAutosuggestEditText](https://github.com/what3words/w3w-android-components) allowing easy integration between both components autosuggest and maps.|```addMarkerAtSuggestion(suggestion, markerColor.RED, { marker -> }, { error -> })```<br>or add multiple suggestions to the map: <br>```addMarkerAtSuggestion(suggestions, markerColor.RED, { markers -> }, { error -> })```|
|**removeMarkerAtSuggestion**, remove Suggestion from the map if exists.|```removeMarkerAtSuggestion(suggestion)```<br>or remove multiple suggestions: <br>```removeMarkerAtSuggestion(suggestions)```|
|**selectAtSuggestion**, set Suggestion as the selected marker on the map. It can only have one selected marker at a time.|```selectAtSuggestion(suggestion, { selectedmarker -> }, { error -> })```|
|**addMarkerAtCoordinates**, add a marker at coordinates to the map. This method will add a marker/square to the map based on the latitude and longitude provided.|```addMarkerAtCoordinates(49.180803, -8.001330, { marker -> }, { error -> })```<br>or add multiple Coordinates to the map: <br>```addMarkerAtCoordinates(listOf(Pair(49.180803, -8.001330), Pair(50.180803, -8.001330)), { markers -> }, { error -> }) ```
|**selectAtCoordinates**, set coordinates as selected marker on the map, it can only have one selected marker at the time.|```selectAtCoordinates(50.180803, -8.001330, { selectedMarker -> }, { error -> }```|
|**findMarkerByCoordinates**, find a marker added to the map (returns null if no marker is added on the specified coordinates).|```val marker = findMarkerByCoordinates(50.180803, -8.001330)```|
|**removeMarkerAtCoordinates**, remove a marker at coordinates from the map (if it exists).|```removeMarkerAtCoordinates(50.180803, -8.001330) ```<br>or remove multiple markers at coordinates from the map: <br>```removeMarkerAtCoordinates(listOf(Pair(49.180803, -8.001330), Pair(50.180803, -8.001330)) ```|
|**addMarkerAtWords**, add a what3words address to the map. This method will add a marker/square to the map if the parameter is valid what3words address, e.g., filled.count.soap, if it's not valid, onError will be called returning APIResponse.What3WordsError.BAD_WORDS.|```addMarkerAtWords("filled.count.soap"), markerColor.RED, { marker -> }, { error -> })```<br>or add multiple 3 word addresses to the map: <br>```addMarkerAtWords(listOf("filled.count.soap", "index.home.raft"), markerColor.RED, { markers -> }, { error -> })```|
|**selectAtWords**, set what3words address as the selected marker on the map. It can only have one selected marker at a time.|```selectAtWords("filled.count.soap", { selectedMarker -> }, { error-> })```|
|**removeMarkerAtWords**, remove a marker at what3words address from the map (if it exists).|```removeMarkerAtWords("filled.count.soap")```<br>or remove multiple markers at three word adresses from the map: <br>```removeMarkerAtWords(listOf("filled.count.soap", "index.home.raft")) ```|
|**removeAllMarkers**, remove all markers added to the map. |```removeAllMarkers()```|
|**getAllMarkers**, Gets all added markers from the map. | ```val markers = getAllMarkers()```|
|**unselect**, remove the selected marker from the map.|```unselect()```|
|**updateMap**, this method should be called on GoogleMap.setOnCameraIdleListener or MapboxMap.addOnMapIdleListener. This method will allow to refresh the grid bounds and draw the grid (if enabled) on camera idle.|```updateMap()```<br>*
mandatory if gridEnabled is set to true (default)* |
|**updateMove**, This method should be called on GoogleMap.setOnCameraMoveListener or MapboxMap.addOnCameraChangeListener. This method will allow swapping from markers to squares and show/hide grid when zoom goes higher or lower than the zoom level threshold (can differ per map provider).|```updateMove()```<br>*
mandatory if gridEnabled is set to true (default)* |


<br><br>
## Map Fragment

Google Map

Since you are creating a new app, you can always opt to use our W3WGoogleMapFragment. The main
advantage is that all the required events to draw the grid are done under the hood, resulting in
less boilerplate code and still having access to the Google Map to apply standard customization (
i.e. map types, etc.)

To use the what3words Google Maps Fragment in your app, first follow the quick start tutorial on the
Google developer portal here:  https://developers.google.com/maps/documentation/android-sdk/start.
This ensures that Google Maps can be used with the what3words Fragment.

After a successful Google Maps run, you can start using our W3WGoogleMapFragment following these
steps:

activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto" android:id="@+id/map"
    android:name="com.what3words.components.maps.views.W3WGoogleMapFragment"
    android:layout_width="match_parent" android:layout_height="match_parent" />
```

Kotlin

```Kotlin
class MainActivity : AppCompatActivity(), W3WMapFragment.OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as W3WGoogleMapFragment
        mapFragment.apiKey("YOUR_API_KEY_HERE", this)
    }

    override fun onMapReady(map: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        map.setLanguage("en")

        //example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        map.addMarkerAtWords(
            words = "filled.count.soap",
            markerColor = W3WMarkerColor.BLUE,
            zoomOption = W3WZoomOption.CENTER_AND_ZOOM,
            onSuccess = { marker ->
                Log.i(
                    "MainActivity",
                    "added ${marker.words} at ${marker.coordinates.lat}, ${marker.coordinates.lng}"
                )
            },
            onError = { error ->
                Log.e(
                    "MainActivity",
                    "${error.key}, ${error.message}"
                )
            }
        )

        map.onSquareSelected(
            onSuccess = { square, selectedByTouch, isMarked ->
                Log.i(
                    "MainActivity",
                    "square selected with words ${square.words}, was it touch? $selectedByTouch, is the square marked? $isMarked"
                )
                search.setSuggestionWithCoordinates(square)
            },
            onError = {
                Toast.makeText(
                    this,
                    "${it.key}, ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
        
        //if you want to access the google map instance inside W3WGoogleMapFragment do the following
        (map as? W3WGoogleMapFragment.Map)?.googleMap()?.let { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }
}
```

MapBox

Since you are creating a new app, you can always opt to use our W3WMapboxMapFragment. The main
advantage is that all the required events to draw the grid are done under the hood, resulting in
less boilerplate code and still having access to the Mapbox Map to apply standard customization (
i.e. map types, etc.)

To use the what3words Mapbox Maps Fragment in your app, first follow the quick start tutorial on the
Mapbox developer portal here: https://docs.mapbox.com/android/navigation/guides/get-started/install/
. This ensures that Mapbox Maps can be used with the what3words Fragment.

After a successful Mapbox map run, you can start using our MapboxWrapper following these steps:

activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto" android:id="@+id/map"
    android:name="com.what3words.components.maps.views.W3WMapboxMapFragment"
    android:layout_width="match_parent" android:layout_height="match_parent" />
```

Kotlin

```Kotlin
class MainActivity : AppCompatActivity(), W3WMapFragment.OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as W3WMapboxMapFragment
        mapFragment.apiKey("YOUR_API_KEY_HERE", this)
    }

    override fun onMapReady(map: W3WMap) {
        //set language to get all the 3wa in the desired language (default english)
        map.setLanguage("en")

        //example how to add a blue marker on a valid 3 word address and move camera to the added marker.
        map.addMarkerAtWords(
            words = "filled.count.soap",
            markerColor = W3WMarkerColor.BLUE,
            zoomOption = W3WZoomOption.CENTER_AND_ZOOM,
            onSuccess = { marker ->
                Log.i(
                    "MainActivity",
                    "added ${marker.words} at ${marker.coordinates.lat}, ${marker.coordinates.lng}"
                )
            },
            onError = { error ->
                Log.e(
                    "MainActivity",
                    "${error.key}, ${error.message}"
                )
            }
        )

        map.onSquareSelected(
            onSuccess = { square, selectedByTouch, isMarked ->
                Log.i(
                    "MainActivity",
                    "square selected with words ${square.words}, was it touch? $selectedByTouch, is the square marked? $isMarked"
                )
                search.setSuggestionWithCoordinates(square)
            },
            onError = {
                Log.e(
                    "MainActivity",
                    "${it.key}, ${it.message}"
                )
            }
        )

        //if you want to access the mapbox map instance inside W3WMapboxMapFragment do the following
        (map as? W3WMapboxMapFragment.Map)?.mapBoxMap()?.let { mapBoxMap ->
            mapBoxMap.loadStyleUri(Style.MAPBOX_STREETS)
        }
    }
}
```

## General map fragment functions:

| Name - Summary | Example |
|---|---|
|**setLanguage**, set the language of what3words address that the onSuccess callback should return. The parameter should be a supported what3words address language as an [ISO 639-1 2 letter code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes). Defaults to en (English).|```setLanguage("en")```|
|**onSquareSelected**, a callback for when an square in the map is selected.|```onSquareSelected { square, selectedByTouch, isMarked -> }```|
|**addMarkerAtSuggestion**, add a suggestion to the map. This method will add a marker/square to the map after getting the Suggestion from our [W3WAutosuggestEditText](https://github.com/what3words/w3w-android-components) allowing easy integration between both components autosuggest and maps.|```addMarkerAtSuggestion(suggestion, markerColor.RED, { marker -> }, { error -> })```<br>or add multiple suggestions to the map: <br>```addMarkerAtSuggestion(suggestions, markerColor.RED, { markers -> }, { error -> })```|
|**removeMarkerAtSuggestion**, remove Suggestion from the map if exists.|```removeMarkerAtSuggestion(suggestion)```<br>or remove multiple suggestions: <br>```removeMarkerAtSuggestion(suggestions)```|
|**selectAtSuggestion**, set Suggestion as the selected marker on the map. It can only have one selected marker at a time.|```selectAtSuggestion(suggestion, { selectedmarker -> }, { error -> })```|
|**addMarkerAtCoordinates**, add a marker at coordinates to the map. This method will add a marker/square to the map based on the latitude and longitude provided.|```addMarkerAtCoordinates(49.180803, -8.001330, { marker -> }, { error -> })```<br>or add multiple Coordinates to the map: <br>```addMarkerAtCoordinates(listOf(Pair(49.180803, -8.001330), Pair(50.180803, -8.001330)), { markers -> }, { error -> }) ```
|**selectAtCoordinates**, set coordinates as selected marker on the map, it can only have one selected marker at the time.|```selectAtCoordinates(50.180803, -8.001330, { selectedMarker -> }, { error -> }```|
|**findMarkerByCoordinates**, find a marker added to the map (returns null if no marker is added on the specified coordinates).|```val marker = findMarkerByCoordinates(50.180803, -8.001330)```|
|**removeMarkerAtCoordinates**, remove a marker at coordinates from the map (if it exists).|```removeMarkerAtCoordinates(50.180803, -8.001330) ```<br>or remove multiple markers at coordinates from the map: <br>```removeMarkerAtCoordinates(listOf(Pair(49.180803, -8.001330), Pair(50.180803, -8.001330)) ```|
|**addMarkerAtWords**, add a what3words address to the map. This method will add a marker/square to the map if the parameter is valid what3words address, e.g., filled.count.soap, if it's not valid, onError will be called returning APIResponse.What3WordsError.BAD_WORDS.|```addMarkerAtWords("filled.count.soap"), markerColor.RED, { marker -> }, { error -> })```<br>or add multiple 3 word addresses to the map: <br>```addMarkerAtWords(listOf("filled.count.soap", "index.home.raft"), markerColor.RED, { markers -> }, { error -> })```|
|**selectAtWords**, set what3words address as the selected marker on the map. It can only have one selected marker at a time.|```selectAtWords("filled.count.soap", { selectedMarker -> }, { error-> })```|
|**removeMarkerAtWords**, remove a marker at what3words address from the map (if it exists).|```removeMarkerAtWords("filled.count.soap")```<br>or remove multiple markers at three word adresses from the map: <br>```removeMarkerAtWords(listOf("filled.count.soap", "index.home.raft")) ```|
|**removeAllMarkers**, remove all markers added to the map. |```removeAllMarkers()```|
|**getAllMarkers**, Gets all added markers from the map. | ```val markers = getAllMarkers()```|
|**unselect**, remove the selected marker from the map.|```unselect()```|
