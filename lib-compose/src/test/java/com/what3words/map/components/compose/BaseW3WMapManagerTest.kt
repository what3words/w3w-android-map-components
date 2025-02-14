package com.what3words.map.components.compose

import android.graphics.PointF
import com.google.android.gms.maps.CameraUpdateFactory
import com.what3words.components.compose.maps.MapProvider
import com.what3words.components.compose.maps.W3WMapManager
import com.what3words.components.compose.maps.models.W3WMapProjection
import com.what3words.components.compose.maps.state.W3WButtonsState
import com.what3words.components.compose.maps.state.W3WMapState
import com.what3words.components.compose.maps.state.camera.W3WCameraState
import com.what3words.core.datasource.text.W3WTextDataSource
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

open class BaseW3WMapManagerTest {
    protected lateinit var mapManager: W3WMapManager
    protected val textDataSource: W3WTextDataSource = mockk()
    protected val testDispatcher = StandardTestDispatcher()
    private val cameraStateMock: W3WCameraState<Any> = mockk(relaxed = true)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mapManager = W3WMapManager(
            mapProvider = MapProvider.GOOGLE_MAP,
            initialMapState = W3WMapState(cameraState = cameraStateMock),
            initialButtonState = W3WButtonsState(
                selectedScreenLocation = mockk(relaxed = true),
                mapProjection = mockk(relaxed = true),
                mapViewPort = mockk(relaxed = true),
                recallButtonViewPort = mockk(relaxed = true),
                recallButtonPosition = mockk(relaxed = true)
            )
        ).apply {
            setTextDataSource(textDataSource)
        }

        // Mock CameraUpdateFactory's behavior
        coEvery { cameraStateMock.moveToPosition(any(), any(), any(), any(), any()) } returns Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(CameraUpdateFactory::class)
    }
}