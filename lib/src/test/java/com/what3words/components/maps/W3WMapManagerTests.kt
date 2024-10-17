package com.what3words.components.maps

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.util.Consumer
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.what3words.androidwrapper.datasource.text.api.error.BadCoordinatesError
import com.what3words.androidwrapper.datasource.text.api.error.BadWordsError
import com.what3words.androidwrapper.datasource.text.api.error.InvalidKeyError
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.wrappers.W3WMapManager
import com.what3words.components.maps.wrappers.W3WMapWrapper
import com.what3words.core.datasource.text.W3WTextDataSource
import com.what3words.core.types.common.W3WError
import com.what3words.core.types.common.W3WResult
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WRFC5646Language
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var manager: W3WMapManager

    @MockK
    private lateinit var dataSource: W3WTextDataSource

    @MockK
    private var wrapper = mockk<W3WMapWrapper>()

    @MockK
    private var suggestionCallback: Consumer<W3WAddress> = mockk()

    @MockK
    private var suggestionsCallback: Consumer<List<W3WAddress>> = mockk()

    @MockK
    private var errorCallback: Consumer<W3WError> = mockk()

    @get:Rule
    internal var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val filledCountSoap = W3WAddress(
        center = W3WCoordinates(51.520847, -0.195521),
        country = W3WCountry("GB"),
        language = W3WRFC5646Language.EN_GB,
        nearestPlace = "Bayswater, London",
        words = "filled.count.soap",
        square = W3WRectangle(
            southwest = W3WCoordinates(51.520833, -0.195543),
            northeast = W3WCoordinates(51.52086, -0.195499)
        )
    )

    private val indexHomeRaft = W3WAddress(
        center = W3WCoordinates(51.521251, -0.203586),
        country = W3WCountry("GB"),
        language = W3WRFC5646Language.EN_GB,
        nearestPlace = "Bayswater, London",
        words = "index.home.raft",
        square = W3WRectangle(
            southwest = W3WCoordinates(51.521238, -0.203607),
            northeast = W3WCoordinates(51.521265, -0.203564)
        )
    )

    private val limitBroomFade = W3WAddress(
        center = W3WCoordinates(51.675062, 0.323787),
        country = W3WCountry("GB"),
        language = W3WRFC5646Language.EN_GB,
        nearestPlace = "Ingatestone, Essex",
        words = "limit.broom.fade",
        square = W3WRectangle(
            southwest = W3WCoordinates(51.675049, 0.323765),
            northeast = W3WCoordinates(51.675075, 0.323808)
        )
    )

    @Before
    fun setup() {
        dataSource = mockk()
        manager = W3WMapManager(
            dataSource,
            wrapper,
            coroutinesTestRule.testDispatcherProvider
        )
        justRun {
            suggestionCallback.accept(any())
            suggestionsCallback.accept(any())
            errorCallback.accept(any())
            wrapper.updateMap()
            wrapper.updateMove()
        }
    }

    @Test
    fun addAndRemoveByCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            //when adding coordinates
            manager.addCoordinates(
                coordinates,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == filledCountSoap.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().first().words == filledCountSoap.words).isNotNull()

            //when removing existing coordinates
            manager.removeCoordinates(
                coordinates
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing coordinates
            manager.removeCoordinates(
                coordinates
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addByCoordinatesError() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            val error = InvalidKeyError("Invalid key", "Invalid key")
            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Failure(InvalidKeyError("Invalid key", "Invalid key"), null)
            }

            //when
            manager.addCoordinates(
                coordinates,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.getList().isEmpty()).isTrue()
        }
    }

    @Test
    fun addAndRemoveMultipleCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            val coordinates2 = W3WCoordinates(51.521251, -0.203586)
            val coordinates3 = W3WCoordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(indexHomeRaft)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) },
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(match { it.size == 3 }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().count()).isEqualTo(3)

            //when removing existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) }
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) }
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addMultipleCoordinatesAndClearList() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            val coordinates2 = W3WCoordinates(51.521251, -0.203586)
            val coordinates3 = W3WCoordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(indexHomeRaft)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) },
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(match { it.size == 3 }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().count()).isEqualTo(3)

            //when clearList
            manager.listOfAddressesToRemove.clear()
            manager.clearList()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when try to clear empty list
            manager.listOfAddressesToRemove.clear()
            manager.clearList()

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addMultipleCoordinatesAndFindOneByLatLng() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            val coordinates2 = W3WCoordinates(51.521251, -0.203586)
            val coordinates3 = W3WCoordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(indexHomeRaft)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) },
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(any()) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().count()).isEqualTo(3)

            //when
            var searchRes = manager.findByExactLocation(coordinates.lat, coordinates.lng)

            //then
            assertThat(searchRes).isNotNull()
            assertThat(searchRes!!.address.words == filledCountSoap.words).isTrue()

            //when
            searchRes = manager.findByExactLocation(51.23, 2.0)

            //then map should not update, ignore
            assertThat(searchRes).isNull()
        }
    }

    @Test
    fun addMultipleCoordinatesFailsOneCancelAll() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val error = BadCoordinatesError("Bad coordinates", "Bad coordinates")
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            val coordinates2 = W3WCoordinates(51.521251, -0.203586)
            val coordinates3 = W3WCoordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Failure(error)
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { W3WCoordinates(it.lat, it.lng) },
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionsCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun selectAndUnselectByCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            //when
            manager.selectCoordinates(
                coordinates,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == suggestion.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.selectedAddress?.words == suggestion.words).isNotNull()

            //when
            manager.unselect()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.selectedAddress).isNull()
        }
    }

    @Test
    fun selectByCoordinatesFails() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val error = BadCoordinatesError("Bad coordinates", "Bad coordinates")
            val coordinates = W3WCoordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates, W3WRFC5646Language.EN_GB)
            } answers {
                W3WResult.Failure(error)
            }

            //when
            manager.selectCoordinates(
                coordinates,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.selectedAddress).isNull()
        }
    }

    @Test
    fun addAndRemoveByWordsSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"

            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            //when
            manager.addWords(
                words,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == suggestion.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().first().words == suggestion.words).isNotNull()

            //when removing existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeWords(
                words
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeWords(
                words
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addAndRemoveMultipleWordsSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val words = "filled.count.soap"
            val words2 = "index.home.raft"
            val words3 = "limit.broom.fade"
            val listWords = listOf(words, words2, words3)

            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertToCoordinates(words2)
            } answers {
                W3WResult.Success(indexHomeRaft)
            }

            coEvery {
                dataSource.convertToCoordinates(words3)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addWords(
                listWords,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(match { it.size == 3 }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().count()).isEqualTo(3)

            //when removing existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeWords(
                listWords
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.listOfAddressesToRemove.clear()
            manager.removeWords(
                listWords
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addMultipleWordsFailsOneCancelAll() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val error = BadWordsError("Bad words", "Bad words")
            val words = "filled.count.soap"
            val words2 = "index.home.raft"
            val words3 = "limit.broom.fade"
            val listWords = listOf(words, words2, words3)

            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            coEvery {
                dataSource.convertToCoordinates(words2)
            } answers {
                W3WResult.Failure(error)
            }

            coEvery {
                dataSource.convertToCoordinates(words3)
            } answers {
                W3WResult.Success(limitBroomFade)
            }

            //when
            manager.addWords(
                listWords,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionsCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addByWordsError() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val error = InvalidKeyError("Invalid key", "Invalid key")
            val words = "filled.count.soap"

            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Failure(error)
            }

            //when
            manager.addWords(
                words,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.getList().isEmpty()).isTrue()
        }
    }

    @Test
    fun selectAndUnselectByWordsSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"

            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Success(filledCountSoap)
            }

            //when
            manager.selectWords(
                words,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == suggestion.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.selectedAddress?.words == suggestion.words).isNotNull()

            //when
            manager.unselect()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.selectedAddress).isNull()
        }
    }

    @Test
    fun selectByWordsFails() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val error = BadCoordinatesError("Bad coordinates", "Bad coordinates")
            val words = "filled.count.soap"


            coEvery {
                dataSource.convertToCoordinates(words)
            } answers {
                W3WResult.Failure(error)
            }

            //when
            manager.selectWords(
                words,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(error) }
            assertThat(manager.selectedAddress).isNull()
        }
    }
}