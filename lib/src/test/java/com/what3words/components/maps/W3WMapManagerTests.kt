package com.what3words.components.maps

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.GoogleMap
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.what3words.components.maps.models.Either
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerFillColor
import com.what3words.components.maps.models.W3WZoomedInMarkerStyle
import com.what3words.components.maps.models.W3WZoomedOutMarkerStyle
import com.what3words.components.maps.wrappers.W3WMapManager
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
class W3WMapManagerTests {
    @MockK
    private lateinit var dataSource: W3WDataSource

    @MockK
    private lateinit var map: GoogleMap

    @MockK
    private lateinit var context: Context

    @get:Rule
    internal var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        dataSource = mockk()
    }

    @Test
    fun addByCoordinatesSuccess() {
        val suggestionJson =
            ClassLoader.getSystemResource("filled.count.soap.json").readText()
        val suggestion =
            Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
        val wrapper = W3WMapManager(
            dataSource,
            coroutinesTestRule.testDispatcherProvider
        )
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val coordinates = Coordinates(51.520847, -0.195521)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates, any())
            } answers {
                Either.Right(suggestion)
            }

            val res = wrapper.addCoordinates(
                coordinates,
                W3WZoomedOutMarkerStyle.CIRCLE,
                W3WZoomedInMarkerStyle.FILLANDOUTLINE,
                W3WMarkerFillColor.YELLOW
            )
            assertThat(res.isRight).isTrue()
            assertThat(wrapper.getAll().first().words == suggestion.words).isNotNull()
        }
    }

    @Test
    fun addByCoordinatesError() {
        val wrapper = W3WMapManager(
            dataSource,
            coroutinesTestRule.testDispatcherProvider
        )
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val coordinates = Coordinates(51.520847, -0.195521)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates, any())
            } answers {
                Either.Left(APIResponse.What3WordsError.INVALID_KEY)
            }

            val res = wrapper.addCoordinates(
                coordinates,
                W3WZoomedOutMarkerStyle.CIRCLE,
                W3WZoomedInMarkerStyle.FILLANDOUTLINE,
                W3WMarkerFillColor.YELLOW
            )
            assertThat(res.isLeft).isTrue()
            assertThat(wrapper.getAll().isEmpty()).isTrue()
        }
    }
}
