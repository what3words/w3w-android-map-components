package com.what3words.components.maps

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.util.Consumer
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.what3words.androidwrapper.What3WordsAndroidWrapper
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.wrappers.W3WMapManager
import com.what3words.components.maps.wrappers.W3WMapWrapper
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.APIResponse.What3WordsError
import com.what3words.javawrapper.response.ConvertTo3WA
import com.what3words.javawrapper.response.ConvertToCoordinates
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
import retrofit2.Response

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
class W3WMapManagerTests {
    private lateinit var manager: W3WMapManager

    @MockK
    private lateinit var dataSource: What3WordsAndroidWrapper

    @MockK
    private var wrapper = mockk<W3WMapWrapper>()

    @MockK
    private var suggestionCallback: Consumer<SuggestionWithCoordinates> = mockk()

    @MockK
    private var suggestionsCallback: Consumer<List<SuggestionWithCoordinates>> = mockk()

    @MockK
    private var errorCallback: Consumer<What3WordsError> = mockk()

    @get:Rule
    internal var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val coordinates = Coordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
               val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when adding coordinates
            manager.addCoordinates(
                coordinates.lat, coordinates.lng,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == suggestion.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().first().words == suggestion.words).isNotNull()

            //when removing existing coordinates
            manager.removeCoordinates(
                coordinates.lat, coordinates.lng
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing coordinates
            manager.removeCoordinates(
                coordinates.lat, coordinates.lng
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
            val coordinates = Coordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val response = APIResponse<ConvertTo3WA>(null)
                response.error = What3WordsError.INVALID_KEY
                ConvertTo3WA().apply {
                    setResponse(response)
                }
            }

            //when
            manager.addCoordinates(
                coordinates.lat, coordinates.lng,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(What3WordsError.INVALID_KEY) }
            assertThat(manager.getList().isEmpty()).isTrue()
        }
    }

    @Test
    fun addAndRemoveMultipleCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion2Json =
                ClassLoader.getSystemResource("index.home.raft.json").readText()
            val suggestion2 =
                Gson().fromJson(suggestion2Json, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val coordinates = Coordinates(51.520847, -0.195521)
            val coordinates2 = Coordinates(51.521251, -0.203586)
            val coordinates3 = Coordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion2.country, suggestion2.square, suggestion2.nearestPlace, suggestion2.coordinates, suggestion2.words, suggestion2.language, suggestion2.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { Pair(it.lat, it.lng) },
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
            manager.suggestionsRemoved.clear()
            manager.removeCoordinates(
                listCoordinates.map { Pair(it.lat, it.lng) }
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.suggestionsRemoved.clear()
            manager.removeCoordinates(
                listCoordinates.map { Pair(it.lat, it.lng) }
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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion2Json =
                ClassLoader.getSystemResource("index.home.raft.json").readText()
            val suggestion2 =
                Gson().fromJson(suggestion2Json, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val coordinates = Coordinates(51.520847, -0.195521)
            val coordinates2 = Coordinates(51.521251, -0.203586)
            val coordinates3 = Coordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion2.country, suggestion2.square, suggestion2.nearestPlace, suggestion2.coordinates, suggestion2.words, suggestion2.language, suggestion2.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { Pair(it.lat,it.lng) },
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
            manager.suggestionsRemoved.clear()
            manager.clearList()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when try to clear empty list
            manager.suggestionsRemoved.clear()
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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion2Json =
                ClassLoader.getSystemResource("index.home.raft.json").readText()
            val suggestion2 =
                Gson().fromJson(suggestion2Json, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val coordinates = Coordinates(51.520847, -0.195521)
            val coordinates2 = Coordinates(51.521251, -0.203586)
            val coordinates3 = Coordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion2.country, suggestion2.square, suggestion2.nearestPlace, suggestion2.coordinates, suggestion2.words, suggestion2.language, suggestion2.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { Pair(it.lat, it.lng) },
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
            assertThat(searchRes!!.suggestion.words == suggestion.words).isTrue()

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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val coordinates = Coordinates(51.520847, -0.195521)
            val coordinates2 = Coordinates(51.521251, -0.203586)
            val coordinates3 = Coordinates(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates2).execute()
            } answers {
                val response = APIResponse<ConvertTo3WA>(null)
                response.error = What3WordsError.BAD_COORDINATES
                ConvertTo3WA().apply {
                    setResponse(response)
                }
            }

            coEvery {
                dataSource.convertTo3wa(coordinates3).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when
            manager.addCoordinates(
                listCoordinates.map { Pair(it.lat, it.lng) },
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionsCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(What3WordsError.BAD_COORDINATES) }
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
            val coordinates = Coordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val c3wa = ConvertTo3WA(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c3wa.apply {
                    this.setResponse(APIResponse(Response.success(c3wa)))
                }
            }

            //when
            manager.selectCoordinates(
                coordinates.lat, coordinates.lng,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(match { it.words == suggestion.words }) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.selectedSuggestion?.words == suggestion.words).isNotNull()

            //when
            manager.unselect()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.selectedSuggestion).isNull()
        }
    }

    @Test
    fun selectByCoordinatesFails() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val coordinates = Coordinates(51.520847, -0.195521)
            coEvery {
                dataSource.convertTo3wa(coordinates).execute()
            } answers {
                val response = APIResponse<ConvertTo3WA>(null)
                response.error = What3WordsError.BAD_COORDINATES
                ConvertTo3WA().apply {
                    setResponse(response)
                }
            }

            //when
            manager.selectCoordinates(
                coordinates.lat, coordinates.lng,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(What3WordsError.BAD_COORDINATES) }
            assertThat(manager.selectedSuggestion).isNull()
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
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
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
            manager.suggestionsRemoved.clear()
            manager.removeWords(
                words
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.suggestionsRemoved.clear()
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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion2Json =
                ClassLoader.getSystemResource("index.home.raft.json").readText()
            val suggestion2 =
                Gson().fromJson(suggestion2Json, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"
            val words2 = "index.home.raft"
            val words3 = "limit.broom.fade"
            val listWords = listOf(words, words2, words3)

            coEvery {
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
            }

            coEvery {
                dataSource.convertToCoordinates(words2).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion2.country, suggestion2.square, suggestion2.nearestPlace, suggestion2.coordinates, suggestion2.words, suggestion2.language, suggestion2.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
            }

            coEvery {
                dataSource.convertToCoordinates(words3).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
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
            manager.suggestionsRemoved.clear()
            manager.removeWords(
                listWords
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.suggestionsRemoved.clear()
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
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"
            val words2 = "index.home.raft"
            val words3 = "limit.broom.fade"
            val listWords = listOf(words, words2, words3)

            coEvery {
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
            }

            coEvery {
                dataSource.convertToCoordinates(words2).execute()
            } answers {
                val response = APIResponse<ConvertToCoordinates>(null)
                response.error = What3WordsError.BAD_COORDINATES
                ConvertToCoordinates().apply {
                    setResponse(response)
                }
            }

            coEvery {
                dataSource.convertToCoordinates(words3).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion3.country, suggestion3.square, suggestion3.nearestPlace, suggestion3.coordinates, suggestion3.words, suggestion3.language, suggestion3.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
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
            verify(exactly = 1) { errorCallback.accept(What3WordsError.BAD_WORDS) }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addByWordsError() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val words = "filled.count.soap"

            coEvery {
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val response = APIResponse<ConvertToCoordinates>(null)
                response.error = What3WordsError.INVALID_KEY
                ConvertToCoordinates().apply {
                    setResponse(response)
                }
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
            verify(exactly = 1) { errorCallback.accept(What3WordsError.INVALID_KEY) }
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
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val c2c = ConvertToCoordinates(suggestion.country, suggestion.square, suggestion.nearestPlace, suggestion.coordinates, suggestion.words, suggestion.language, suggestion.map)
                c2c.apply {
                    this.setResponse(APIResponse(Response.success(c2c)))
                }
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
            assertThat(manager.selectedSuggestion?.words == suggestion.words).isNotNull()

            //when
            manager.unselect()

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.selectedSuggestion).isNull()
        }
    }

    @Test
    fun selectByWordsFails() {
        coroutinesTestRule.testDispatcher.dispatch(coroutinesTestRule.testDispatcherProvider.default()) {
            //given
            val words = "filled.count.soap"

            coEvery {
                dataSource.convertToCoordinates(words).execute()
            } answers {
                val response = APIResponse<ConvertToCoordinates>(null)
                response.error = What3WordsError.BAD_COORDINATES
                ConvertToCoordinates().apply {
                    setResponse(response)
                }
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
            verify(exactly = 1) { errorCallback.accept(What3WordsError.BAD_COORDINATES) }
            assertThat(manager.selectedSuggestion).isNull()
        }
    }
}
