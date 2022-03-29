package com.what3words.components.maps.models

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.extensions.toSuggestionWithCoordinates
import com.what3words.javawrapper.request.BoundingBox
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.GridSection
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class W3WApiDataSource(private val wrapper: What3WordsV3, private val context: Context) :
    W3WDataSource {

    val requestQueue = Volley.newRequestQueue(this.context)

    override suspend fun getSuggestionByCoordinates(
        coordinates: Coordinates,
        language: String
    ): Either<APIResponse.What3WordsError, SuggestionWithCoordinates> =
        suspendCoroutine { cont ->
            val c23wa =
                wrapper.convertTo3wa(coordinates)
                    .language(language)
                    .execute()
            if (c23wa.isSuccessful) {
                cont.resume(Either.Right(c23wa.toSuggestionWithCoordinates()))
            } else {
                cont.resume(Either.Left(c23wa.error))
            }
        }

    override suspend fun getSuggestionByWords(
        words: String
    ): Either<APIResponse.What3WordsError, SuggestionWithCoordinates> =
        suspendCoroutine { cont ->
            val c23wa =
                wrapper.convertToCoordinates(words)
                    .execute()
            if (c23wa.isSuccessful) {
                cont.resume(Either.Right(c23wa.toSuggestionWithCoordinates()))
            } else {
                cont.resume(Either.Left(c23wa.error))
            }
        }

    override suspend fun getGrid(boundingBox: BoundingBox): Either<APIResponse.What3WordsError, GridSection> =
        suspendCoroutine { cont ->
            val grid = wrapper.gridSection(
                boundingBox
            ).execute()
            if (grid.isSuccessful) {
                cont.resume(Either.Right(grid))
            } else {
                cont.resume(Either.Left(grid.error))
            }
        }

    override suspend fun getGeoJsonGrid(boundingBox: BoundingBox): Either<APIResponse.What3WordsError, String> =
        suspendCoroutine { cont ->
            val apiUrl =
                "https://api.what3words.com/v3/grid-section?bounding-box=${boundingBox.sw.lat},${boundingBox.sw.lng},${boundingBox.ne.lat},${boundingBox.ne.lng}&format=geojson&key=TCRPZKEE"
            val jsonObjectRequest = StringRequest(
                Request.Method.GET, apiUrl,
                {
                    cont.resume(Either.Right(it))
                },
                {
                    cont.resume(
                        Either.Left(
                            APIResponse.What3WordsError.UNKNOWN_ERROR.apply {
                                this.message = it.message
                            }
                        )
                    )
                }
            )
            requestQueue.add(jsonObjectRequest)
        }
}
