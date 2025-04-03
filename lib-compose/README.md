# <img src="https://what3words.com/assets/images/w3w_square_red.png" width="64" height="64" alt="what3words">&nbsp;w3w-android-map-components-compose

[![Maven Central](https://img.shields.io/maven-central/v/com.what3words/w3w-android-map-components-compose.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.what3words%22%20AND%20a:%22w3w-android-map-components-compose%22)

### Android minimum SDK support

[![Generic badge](https://img.shields.io/badge/minSdk-24-green.svg)](https://developer.android.com/about/versions/nougat/android-7.0)

# Table of Contents
- [ w3w-android-map-components-compose](#w3w-android-map-components-compose)
    - [Android minimum SDK support](#android-minimum-sdk-support)
- [Table of Contents](#table-of-contents)
  - [I. Introduction](#i-introduction)
  - [II. Installation](#ii-installation)
    - [Get API Key](#get-api-key)
    - [Gradle](#gradle)
  - [III. Usage](#iii-usage)
    - [1. Creating Maps with what3words](#1-creating-maps-with-what3words)
      - [Implementation](#implementation)
      - [Configuration](#configuration)
        - [Map Configuration](#map-configuration)
        - [Button Configuration](#button-configuration)
        - [Grid Line Appearance](#grid-line-appearance)
        - [Layout Configuration](#layout-configuration)
        - [Map Colors Configuration](#map-colors-configuration)
    - [2. Adding what3words to Existing Maps](#2-adding-what3words-to-existing-maps)
      - [Google Map](#google-map)
        - [Precondition: Assume that your GoogleMap Compose is already set up and that you're handling the cameraPositionState](#precondition-assume-that-your-googlemap-compose-is-already-set-up-and-that-youre-handling-the-camerapositionstate)
        - [Step 1: Now, for the interaction part—first, set up the initialization.](#step-1-now-for-the-interaction-partfirst-set-up-the-initialization)
        - [Step 2: Then, update cameraState in map manager and helps function for drawing gridline](#step-2-then-update-camerastate-in-map-manager-and-helps-function-for-drawing-gridline)
        - [Step 3: Finally, set the selected marker on the map and use W3WGoogleMapDrawer to render the gridlines and markers.](#step-3-finally-set-the-selected-marker-on-the-map-and-use-w3wgooglemapdrawer-to-render-the-gridlines-and-markers)
      - [MapBox](#mapbox)
        - [Precondition: Assume that your MapBox Compose is already set up and that you're handling the MapViewPortState](#precondition-assume-that-your-mapbox-compose-is-already-set-up-and-that-youre-handling-the-mapviewportstate)
        - [Step 1: Now, for the interaction part—first, set up the initialization.](#step-1-now-for-the-interaction-partfirst-set-up-the-initialization-1)
        - [Step 2: Then, update cameraState in map manager and helps function for drawing gridline](#step-2-then-update-camerastate-in-map-manager-and-helps-function-for-drawing-gridline-1)
        - [Step 3: Finally, set the selected marker on the map and use W3WMapBoxDrawers to render the gridlines and markers.](#step-3-finally-set-the-selected-marker-on-the-map-and-use-w3wmapboxdrawers-to-render-the-gridlines-and-markers)
  - [IV. W3WMapManager functions:](#iv-w3wmapmanager-functions)
    - [General and Camera Control](#general-and-camera-control)
    - [Marker Management](#marker-management)
    - [Button Controls](#button-controls)
  - [V. Samples](#v-samples)
    - [Sample for Mapbox](#sample-for-mapbox)
    - [Sample for GoogleMap](#sample-for-googlemap)


  
## I. Introduction

The what3words Map Component for Jetpack Compose provides a straightforward way to add what3words functionality to maps in your Jetpack Compose applications. 
With this library, you can display features such as the what3words grid and what3words markers with what3words addresses.

Our library offers two flexible integration approaches to suit different development needs:

1. **Using a complete map with what3words** - If you need a component that includes both the map and what3words functionality built-in, use our [`W3WMapComponent`](../lib-compose/src/main/java/com/what3words/components/compose/maps/W3WMapComponent.kt).

   This component includes the map itself with handled camera state and allows for switching between map providers (Google Maps or Mapbox). It supports drawing the 3x3m grid lines, markers, and selected markers on the map using what3words address data. Additionally, it provides built-in UI controls for recalling selected positions, accessing device location, and switching between map types. See [Creating Maps with what3words](#1-creating-maps-with-what3words) for implementation details.

2. **Adding what3words to an existing map** - If you already have a map in your application, use our map drawers: [`W3WGoogleMapDrawer`](../lib-compose/src/main/java/com/what3words/components/compose/maps/providers/googlemap/W3WGoogleMapDrawer.kt) for Google Maps or [`W3WMapBoxDrawer`](../lib-compose/src/main/java/com/what3words/components/compose/maps/providers/mapbox/W3WMapBoxDrawer.kt) for Mapbox.

   These drawers support drawing the 3x3m grid lines, markers, and selected markers on the map using what3words address data. They also include configurable button layouts for features like position recall, current location access, and map type switching. See [Adding what3words to Existing Maps](#2-adding-what3words-to-existing-maps) for details.
   
## II. Installation

### Get API Key

To obtain an API key, please
visit [https://what3words.com/select-plan](https://what3words.com/select-plan) and sign up for an
account.

### Gradle

To integrate the what3words Map Component for Jetpack Compose into your project, add the following dependency to your app's `build.gradle` file:

```groovy
dependencies {
    // what3words Map Component for Compose
    implementation 'com.what3words:w3w-android-map-components-compose:$latest'
}
```

## III. Usage

The what3words Map Component for Compose offers three flexible approaches for integration:

### 1. Creating Maps with what3words
The simplest approach, using our all-in-one map component with what3words functionality built-in. Ideal for new projects or screens where you need a complete mapping solution.

The `W3WMapComponent` provides a complete solution for adding maps with what3words functionality to your Jetpack Compose application. This component handles both the map rendering and what3words features like grid display, markers, and address conversion.

#### Implementation

To add a map with what3words functionality to your Compose UI:

```kotlin
    // 1. Create text data source
    val textDataSource = W3WApiTextDataSource.create(LocalContext.current, "YOUR_API_KEY_HERE")
    
    // 2. Create map manager
    val mapManager = rememberW3WMapManager(
        mapProvider = MapProvider.GOOGLE_MAP  // or MapProvider.MAPBOX
    )
    
    // 3. Create location source (optional)
    val locationSource = remember {
        LocationSourceImpl(LocalContext.current)
    }
    
    // 4. Add the W3WMapComponent to your UI
    W3WMapComponent(
        modifier = Modifier.fillMaxSize(),
        mapManager = mapManager,
        textDataSource = textDataSource,
        locationSource = locationSource,  // Optional
        onSelectedSquareChanged = { address ->
            // Handle when user selects a what3words square
        }
    )
```

> Note: The locationSource parameter enables device location features in your map component. This allows users to see their current position and navigate to it through the My Location button.

#### Configuration

##### Map Configuration

You can customize various aspects of the map to match your application's needs:


```kotlin
W3WMapComponent(
    // ...other parameters
    mapConfig = W3WMapDefaults.defaultMapConfig(
        darkModeCustomJsonStyle = null,  // Custom JSON style for dark mode (Mapbox)
        isBuildingEnable = true,  // Enable 3D building display
        isCompassButtonEnabled = true  // Show compass button on the map
    )
)
```

##### Button Configuration
Control which interactive buttons appear on the map:
```kotlin
W3WMapComponent(
    // ...other parameters
    mapConfig = W3WMapDefaults.defaultMapConfig(
        buttonConfig = W3WMapDefaults.ButtonConfig(
            isMapSwitchFeatureEnabled = true,  // Button to switch map types (default: true)
            isMyLocationFeatureEnabled = true,  // Button to focus on user's location (default: true)
            isRecallFeatureEnabled = false  // Button to return to selected location (default: false)
        )
    )
)
```

##### Grid Line Appearance
Adjust the appearance of the what3words grid:
```kotlin
W3WMapComponent(
    // ...other parameters
    mapConfig = W3WMapDefaults.defaultMapConfig(
        gridLineConfig = W3WMapDefaults.GridLinesConfig(
            isGridEnabled = true,  // Enable grid visibility (default: true)
            zoomSwitchLevel = 17.5f,  // Zoom level where grid appears (default: DEFAULT_MAP_ZOOM_SWITCH_LEVEL)
            gridLineWidth = 2.dp,  // Width of grid lines (default: 2.dp)
            gridScale = 6f  // Scale factor for grid display (default: 6f)
        )
    )
)
```

##### Layout Configuration
Customize the layout of the map and its UI elements:

```kotlin
W3WMapComponent(
    // ...other parameters
    layoutConfig = W3WMapDefaults.defaultLayoutConfig(
        contentPadding = PaddingValues(bottom = 24.dp, end = 8.dp),  // Default padding for content
        buttonsLayoutConfig = W3WMapDefaults.defaultButtonsLayoutConfig()  // Configuration for button layout
    )
)
```

##### Map Colors Configuration

Customize the colors used for different map types:

```kotlin
W3WMapComponent(
    // ...other parameters
    mapColors = W3WMapDefaults.defaultMapColors(
        normalMapColor = W3WMapDefaults.defaultNormalMapColor(),  // Colors for standard map view
        darkMapColor = W3WMapDefaults.defaultDarkMapColor(),  // Colors for dark mode map
        satelliteMapColor = W3WMapDefaults.defaultSatelliteMapColor()  // Colors for satellite view
``` 

Detail for config color:
```kotlin
    mapColors = W3WMapDefaults.defaultMapColors(
        normalMapColor = W3WMapDefaults.MapColor(
            gridLineColor = Color(0xFF888888),
            selectedGridLineColor = Color(0xFFE10000),
            markerSlashColor = Color.White,
            markerBackgroundColor = Color(0xFFE10000)
        ),
    )
``` 

### 2. Adding what3words to Existing Maps

#### Google Map

##### Precondition: Assume that your GoogleMap Compose is already set up and that you're handling the cameraPositionState
``` kotlin
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }
    
    //existing code here...
    
    GoogleMap(
      modifier = modifier,
      cameraPositionState = cameraPositionState,
      //existing code here...
    ) {
        //existing code here...
    }
``` 

##### Step 1: Now, for the interaction part—first, set up the initialization.
``` kotlin
// --- Setup map manager, text datasource and state ---
// Create textdata source
val textDataSource = W3WApiTextDataSource.create(this, BuildConfig.W3W_API_KEY)

// Create map config
val mapConfig = W3WMapDefaults.defaultMapConfig()

// Create map manager    
val mapManager = rememberW3WMapManager(
    mapProvider = MapProvider.GOOGLE_MAP,
).apply {
    setMapConfig(mapConfig)
    setTextDataSource(textDataSource)
}

// Collect map state from manager
val mapState by mapManager.mapState.collectAsState()
``` 

##### Step 2: Then, update cameraState in map manager and helps function for drawing gridline

``` kotlin
// Update the reference for init and after rotation
DisposableEffect(cameraPositionState) {
  val w3wGoogleCameraState = mapManager.mapState.value.cameraState as? W3WGoogleCameraState
  w3wGoogleCameraState?.cameraState = cameraPositionState
  onDispose { }
}

// Handling get gridBound and visibleBound needed to draw the 3x3m grid on the map
val w3wGoogleCameraState = mapState.cameraState as W3WGoogleCameraState
var lastProcessedPosition by remember { mutableStateOf(cameraPositionState.position) }

LaunchedEffect(cameraPositionState) {
    snapshotFlow { cameraPositionState.position to cameraPositionState.projection }
        .conflate()
        .onEach { (position, projection) ->
            projection?.let {
                updateCameraBound(
                    projection,
                    mapConfig.gridLineConfig
                ) { gridBound, visibleBound ->
                    lastProcessedPosition = position
                    w3wGoogleCameraState.gridBound = gridBound
                    w3wGoogleCameraState.visibleBound = visibleBound
                    coroutineScope.launch {
                        mapManager.updateCameraState(w3wGoogleCameraState)
                    }
                }
            }
        }.launchIn(this)
}
//endregion
``` 

##### Step 3: Finally, set the selected marker on the map and use W3WGoogleMapDrawer to render the gridlines and markers.
``` kotlin
GoogleMap(
    modifier = modifier,
    cameraPositionState = cameraPositionState,
    //existing code here...
    onMapClick = {
      coroutineScope.launch {
        // Needed to draw selected marker when click on map
        mapManager.setSelectedAt(W3WCoordinates(it.latitude, it.longitude))
      }
    },
    mapColorScheme = darkMode
) {
    //existing code here...
    
    // Needed to draw the 3x3m grid on the map and select markers
    W3WGoogleMapDrawer(
      state = mapState,
      mapConfig = mapConfig,
      mapColor = W3WMapDefaults.defaultNormalMapColor(),
      onMarkerClicked = {
          coroutineScope.launch {
              mapManager.setSelectedAt(it.center)
          }
      }
    )
}
```
---

#### MapBox

##### Precondition: Assume that your MapBox Compose is already set up and that you're handling the MapViewPortState
``` kotlin
val mapViewportState = rememberMapViewportState {
    setCameraOptions(
      CameraOptions.Builder()
        .center(Point.fromLngLat(london1Coordinate.lng, london1Coordinate.lat))
        .zoom(19.0)
        .bearing(0.0)
        .pitch(0.0)
        .build()
    )
}

//existing code here...

MapboxMap(
    modifier = modifier,
    mapViewportState = mapViewportState,
  //existing code here...
) {
    //existing code here...
}
```

##### Step 1: Now, for the interaction part—first, set up the initialization.
``` kotlin
// --- Setup map manager, text datasource and state ---
// Create textdata source
val textDataSource = W3WApiTextDataSource.create(this, BuildConfig.W3W_API_KEY)

// Create map config
val mapConfig = W3WMapDefaults.defaultMapConfig()

// Create instance for mapview
var mapView: MapView? by remember {
    mutableStateOf(null)
}

// --- Create map manager ---
val mapManager = rememberW3WMapManager(
    mapProvider = MapProvider.MAPBOX,
).apply {
    setMapConfig(mapConfig)
    setTextDataSource(textDataSource)
}

// Collect map state from manager
val mapState by mapManager.mapState.collectAsState()
```

##### Step 2: Then, update cameraState in map manager and helps function for drawing gridline
``` kotlin
// Update the reference for init and after rotation
DisposableEffect(mapViewportState) {
    val w3wMapBoxCameraState = mapManager.mapState.value.cameraState as? W3WMapboxCameraState
    w3wMapBoxCameraState?.cameraState = mapViewportState
    onDispose { }
}

// Handling get gridBound and visibleBound needed to draw the 3x3m grid on the map
val w3wMapBoxCameraState = mapState.cameraState as W3WMapboxCameraState
var lastProcessedCameraState by remember { mutableStateOf(mapViewportState.cameraState) }

LaunchedEffect(mapViewportState.cameraState) {
    snapshotFlow { mapViewportState.cameraState }
        .filterNotNull()
        .onEach { currentCameraState ->
            mapView?.mapboxMap?.let { mapboxMap ->
                updateGridBound(
                    mapboxMap,
                    mapConfig.gridLineConfig,
                    onCameraBoundUpdate = { gridBound, visibleBound ->
                        lastProcessedCameraState = currentCameraState
                        w3wMapBoxCameraState.gridBound = gridBound
                        w3wMapBoxCameraState.visibleBound = visibleBound
                        coroutineScope.launch {
                            mapManager.updateCameraState(w3wMapBoxCameraState)
                        }
                    }
                )
            }
        }.launchIn(this)
}
```

##### Step 3: Finally, set the selected marker on the map and use W3WMapBoxDrawers to render the gridlines and markers.
``` kotlin
MapboxMap(
    modifier = modifier,
    mapViewportState = mapViewportState,
    onMapClick = {
      coroutineScope.launch {
          // Needed to draw selected marker when click on map
          mapManager.setSelectedAt(W3WCoordinates(it.latitude(), it.longitude()))
      }
    },
    //existing code here...
) {
    //existing code here...
    
    // Needed to calculate gridBound for draw gridline
    MapEffect {
        mapView = it
    }

    // Needed to draw the 3x3m grid on the map and select markers
    W3WMapBoxDrawer(
        state = mapState,
        mapConfig = mapConfig,
        mapColor = W3WMapDefaults.defaultNormalMapColor(),
        onMarkerClicked = {
            coroutineScope.launch {
                mapManager.setSelectedAt(it.center)
            }
        }
    )
}
```

## IV. W3WMapManager functions:

### General and Camera Control

| Function - Summary | Example |
|---|---|
|**setLanguage**, set the language of what3words address that will be returned. The parameter should be a supported what3words address language as an [ISO 639-1 2 letter code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes).|```mapManager.setLanguage("fr")```|
|**getLanguage**, returns the currently set language for what3words addresses.|```val language = mapManager.getLanguage()```|
|**isDarkModeEnabled**, checks if dark mode is currently enabled.|```val isDark = mapManager.isDarkModeEnabled()```|
|**enableDarkMode**, enables or disables dark mode for the map.|```mapManager.enableDarkMode(true)```|
|**getMapType**, gets the current map type.|```val mapType = mapManager.getMapType()```|
|**setMapType**, sets the map type (NORMAL, SATELLITE, etc.).|```mapManager.setMapType(W3WMapType.SATELLITE)```|
|**setMapGesturesEnabled**, enables or disables user gestures on the map.|```mapManager.setMapGesturesEnabled(true)```|
|**setMyLocationEnabled**, enables or disables the "My Location" feature.|```mapManager.setMyLocationEnabled(true)```|
|**moveToPosition**, moves the camera to specified coordinates with optional zoom, bearing, and tilt.|```mapManager.moveToPosition(coordinates, zoom = 17f, animate = true)```|
|**orientCamera**, adjusts camera bearing to 0 (north up).|```mapManager.orientCamera()```|

---


### Marker Management

| Function - Summary | Example |
|---|---|
|**addMarkerAt**, adds a marker at coordinates, what3words address, or suggestion with custom color options. | ```mapManager.addMarkerAt(words = "filled.count.soap", markerColor = W3WMarkerColor.RED)``` <br> ```mapManager.addMarkerAt(coordinates = W3WCoordinates(51.520847, -0.195521))``` <br> ```mapManager.addMarkerAt(suggestion = w3wSuggestion, markerColor = customColor)``` |
|**addMarkersAt**, adds multiple markers using lists of coordinates, addresses, or suggestions with optional list name for organization. | ```mapManager.addMarkersAt(listWords = listOf("filled.count.soap", "index.home.raft"))``` <br> ```mapManager.addMarkersAt(listCoordinates = listOf(coord1, coord2), listName = "Favorites")``` <br> ```mapManager.addMarkersAt(listSuggestions = suggestionsList, markerColor = W3WMarkerColor.BLUE)``` |
|**removeMarkerAt**, removes a marker based on coordinates, what3words address, or suggestion. | ```mapManager.removeMarkerAt(words = "filled.count.soap")``` <br> ```mapManager.removeMarkerAt(coordinates = W3WCoordinates(51.520847, -0.195521))``` <br> ```mapManager.removeMarkerAt(suggestion = w3wSuggestion)``` |
|**removeMarkersAt**, removes multiple markers based on lists of coordinates, addresses, or suggestions. | ```mapManager.removeMarkersAt(listWords = listOf("filled.count.soap", "index.home.raft"))``` <br> ```mapManager.removeMarkersAt(listCoordinates = coordinatesList)``` <br> ```mapManager.removeMarkersAt(listSuggestions = suggestionsList)``` |
|**setSelectedAt**, selects a marker based on coordinates, what3words address, or suggestion. | ```mapManager.setSelectedAt("filled.count.soap")``` <br> ```mapManager.setSelectedAt(coordinates = W3WCoordinates(51.520847, -0.195521))``` <br> ```mapManager.setSelectedAt(suggestion = w3wSuggestion)``` |
|**findMarkerBy**, finds markers by coordinates, what3words address, or suggestion. | ```val marker = mapManager.findMarkerBy("filled.count.soap")``` <br> ```val marker = mapManager.findMarkerBy(coordinates = W3WCoordinates(51.520847, -0.195521))``` |
|**getMarkersAt**, gets markers at specific coordinates or what3words address. | ```val markers = mapManager.getMarkersAt(coordinates = W3WCoordinates(51.520847, -0.195521))``` <br> ```val markers = mapManager.getMarkersAt(words = "filled.count.soap")``` |
|**getMarkersInList**, gets all markers in a specific named list. | ```val markers = mapManager.getMarkersInList("Favorites")``` |
|**getSelectedAddress**, gets the currently selected what3words address. | ```val selected = mapManager.getSelectedAddress()``` |
|**clearSelectedAddress**, clears the currently selected address. | ```mapManager.clearSelectedAddress()``` |
|**removeListMarker**, removes all markers in a specific named list. | ```mapManager.removeListMarker("Favorites")``` |
|**removeAllMarkers**, removes all markers from the map. | ```mapManager.removeAllMarkers()``` |

**Example usage code:**

_Add marker_
``` kotlin
mapManager.addMarkerAt(
    coordinates = W3WCoordinates(51.513678, -0.133823),
    markerColor = W3WMarkerColor(background = Color.Red, slash = Color.Yellow)
)
```

_Add markers_
``` kotlin
const val london3W3WAddress = "gold.basin.freed"
const val london4W3WAddress = "known.format.adults"
const val london5W3WAddress = "piles.hedge.logo"

mapManager.addMarkersAt(
    listWords = listOf(
        london3W3WAddress,
        london4W3WAddress,
        london5W3WAddress
    ),
    listName = "London 2",
    markerColor = W3WMarkerColor(background = Color.Blue, slash = Color.White),
)
```

_Set selected marker on map_
``` kotlin
mapManager.setSelectedAt("filled.count.soap")
```

---


### Button Controls

| Function - Summary | Example |
|---|---|
|**setRecallButtonEnabled**, enables or disables the recall button clickability.|```mapManager.setRecallButtonEnabled(true)```|
|**setRecallButtonVisible**, sets the visibility of the recall button.|```mapManager.setRecallButtonVisible(true)```|
|**setRecallButtonPosition**, sets the position of the recall button on screen.|```mapManager.setRecallButtonPosition(PointF(100f, 100f))```|
|**setMyLocationButtonEnabled**, enables or disables the my location button clickability.|```mapManager.setMyLocationButtonEnabled(true)```|
|**setMyLocationButtonVisible**, sets the visibility of the my location button.|```mapManager.setMyLocationButtonVisible(true)```|
|**setMapSwitchButtonEnabled**, enables or disables the map switch button clickability.|```mapManager.setMapSwitchButtonEnabled(true)```|
|**setMapSwitchButtonVisible**, sets the visibility of the map switch button.|```mapManager.setMapSwitchButtonVisible(true)```|

---

## V. Samples

###  Sample for Mapbox
[mapbox-sample](https://github.com/what3words/w3w-android-samples/tree/main/mapbox-v11-sample)

###  Sample for GoogleMap
[maps-googlemaps-sample](https://github.com/what3words/w3w-android-samples/tree/main/maps-googlemaps-sample)