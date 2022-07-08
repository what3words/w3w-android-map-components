package com.what3words.components.maps

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.GoogleMap
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.what3words.components.maps.models.Either
import com.what3words.components.maps.models.W3WDataSource
import com.what3words.components.maps.models.W3WMarkerColor
import com.what3words.components.maps.wrappers.W3WMapManager
import com.what3words.components.maps.wrappers.W3WMapWrapper
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
import androidx.core.util.Consumer
import io.mockk.justRun
import io.mockk.verify
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
    private lateinit var dataSource: W3WDataSource

    @MockK
    private var wrapper = mockk<W3WMapWrapper>()

    @MockK
    private var suggestionCallback: Consumer<SuggestionWithCoordinates> = mockk()

    @MockK
    private var suggestionsCallback: Consumer<List<SuggestionWithCoordinates>> = mockk()

    @MockK
    private var errorCallback: Consumer<APIResponse.What3WordsError> = mockk()

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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)

            coEvery {
                dataSource.getSuggestionByCoordinates(51.520847, -0.195521, any())
            } answers {
                Either.Right(suggestion)
            }

            //when adding coordinates
            manager.addCoordinates(
                51.520847, -0.195521,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(suggestion) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().first().words == suggestion.words).isNotNull()

            //when removing existing coordinates
            manager.removeCoordinates(
                51.520847, -0.195521
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing coordinates
            manager.removeCoordinates(
                51.520847, -0.195521
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addByCoordinatesError() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            coEvery {
                dataSource.getSuggestionByCoordinates(51.520847, -0.195521, any())
            } answers {
                Either.Left(APIResponse.What3WordsError.INVALID_KEY)
            }

            //when
            manager.addCoordinates(
                51.520847, -0.195521,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.INVALID_KEY) }
            assertThat(manager.getList().isEmpty()).isTrue()
        }
    }

    @Test
    fun addAndRemoveMultipleCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
            val coordinates = Pair(51.520847, -0.195521)
            val coordinates2 = Pair(51.521251, -0.203586)
            val coordinates3 = Pair(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates.first, coordinates.second, any())
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates2.first, coordinates2.second, any())
            } answers {
                Either.Right(suggestion2)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates3.first, coordinates3.second, any())
            } answers {
                Either.Right(suggestion3)
            }

            //when
            manager.addCoordinates(
                listCoordinates,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(any()) }
            verify(exactly = 1) { wrapper.updateMap() }
            verify(exactly = 0) { errorCallback.accept(any()) }
            assertThat(manager.getList().count()).isEqualTo(3)

            //when removing existing words
            manager.suggestionsRemoved.clear()
            manager.removeCoordinates(
                listCoordinates
            )

            //then
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()

            //when removing non-existing words
            manager.suggestionsRemoved.clear()
            manager.removeCoordinates(
                listCoordinates
            )

            //then map should not update, ignore
            verify(exactly = 2) { wrapper.updateMap() }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addMultipleCoordinatesAndClearList() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
            val coordinates = Pair(51.520847, -0.195521)
            val coordinates2 = Pair(51.521251, -0.203586)
            val coordinates3 = Pair(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates.first, coordinates.second, any())
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates2.first, coordinates2.second, any())
            } answers {
                Either.Right(suggestion2)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates3.first, coordinates3.second, any())
            } answers {
                Either.Right(suggestion3)
            }

            //when
            manager.addCoordinates(
                listCoordinates,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(any()) }
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
            val coordinates = Pair(51.520847, -0.195521)
            val coordinates2 = Pair(51.521251, -0.203586)
            val coordinates3 = Pair(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates.first, coordinates.second, any())
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates2.first, coordinates2.second, any())
            } answers {
                Either.Right(suggestion2)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates3.first, coordinates3.second, any())
            } answers {
                Either.Right(suggestion3)
            }

            //when
            manager.addCoordinates(
                listCoordinates,
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
            var searchRes = manager.findByExactLocation(coordinates.first, coordinates.second)

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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val suggestion3Json =
                ClassLoader.getSystemResource("limit.broom.fade.json").readText()
            val suggestion3 =
                Gson().fromJson(suggestion3Json, SuggestionWithCoordinates::class.java)
            val coordinates = Pair(51.520847, -0.195521)
            val coordinates2 = Pair(51.521251, -0.203586)
            val coordinates3 = Pair(51.675062, 0.323787)
            val listCoordinates = listOf(coordinates, coordinates2, coordinates3)

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates.first, coordinates.second, any())
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates2.first,coordinates2.second, any())
            } answers {
                Either.Left(APIResponse.What3WordsError.BAD_COORDINATES)
            }

            coEvery {
                dataSource.getSuggestionByCoordinates(coordinates3.first, coordinates3.second, any())
            } answers {
                Either.Right(suggestion3)
            }

            //when
            manager.addCoordinates(
                listCoordinates,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionsCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.BAD_COORDINATES) }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun selectAndUnselectByCoordinatesSuccess() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)

            coEvery {
                dataSource.getSuggestionByCoordinates(51.520847, -0.195521, any())
            } answers {
                Either.Right(suggestion)
            }

            //when
            manager.selectCoordinates(
                51.520847, -0.195521,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(suggestion) }
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            coEvery {
                dataSource.getSuggestionByCoordinates(51.520847, -0.195521, any())
            } answers {
                Either.Left(APIResponse.What3WordsError.BAD_COORDINATES)
            }

            //when
            manager.selectCoordinates(
                51.520847, -0.195521,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 0) { suggestionCallback.accept(any()) }
            verify(exactly = 0) { wrapper.updateMap() }
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.BAD_COORDINATES) }
            assertThat(manager.selectedSuggestion).isNull()
        }
    }

    @Test
    fun addAndRemoveByWordsSuccess() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"

            coEvery {
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Right(suggestion)
            }

            //when
            manager.addWords(
                words,
                W3WMarkerColor.YELLOW,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(suggestion) }
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByWords(words2)
            } answers {
                Either.Right(suggestion2)
            }

            coEvery {
                dataSource.getSuggestionByWords(words3)
            } answers {
                Either.Right(suggestion3)
            }

            //when
            manager.addWords(
                listWords,
                W3WMarkerColor.YELLOW,
                suggestionsCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionsCallback.accept(any()) }
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Right(suggestion)
            }

            coEvery {
                dataSource.getSuggestionByWords(words2)
            } answers {
                Either.Left(APIResponse.What3WordsError.BAD_WORDS)
            }

            coEvery {
                dataSource.getSuggestionByWords(words3)
            } answers {
                Either.Right(suggestion3)
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
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.BAD_WORDS) }
            assertThat(manager.getList()).isEmpty()
        }
    }

    @Test
    fun addByWordsError() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val words = "filled.count.soap"

            coEvery {
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Left(APIResponse.What3WordsError.INVALID_KEY)
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
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.INVALID_KEY) }
            assertThat(manager.getList().isEmpty()).isTrue()
        }
    }

    @Test
    fun selectAndUnselectByWordsSuccess() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val suggestionJson =
                ClassLoader.getSystemResource("filled.count.soap.json").readText()
            val suggestion =
                Gson().fromJson(suggestionJson, SuggestionWithCoordinates::class.java)
            val words = "filled.count.soap"

            coEvery {
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Right(suggestion)
            }

            //when
            manager.selectWords(
                words,
                suggestionCallback,
                errorCallback
            )

            //then
            verify(exactly = 1) { suggestionCallback.accept(suggestion) }
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            //given
            val words = "filled.count.soap"

            coEvery {
                dataSource.getSuggestionByWords(words)
            } answers {
                Either.Left(APIResponse.What3WordsError.BAD_COORDINATES)
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
            verify(exactly = 1) { errorCallback.accept(APIResponse.What3WordsError.BAD_COORDINATES) }
            assertThat(manager.selectedSuggestion).isNull()
        }
    }
}
