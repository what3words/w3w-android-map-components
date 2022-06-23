package com.what3words.components.maps.models

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.what3words.androidwrapper.What3WordsV3
import com.what3words.components.maps.extensions.toSuggestionWithCoordinates
import com.what3words.javawrapper.request.BoundingBox
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.Line
import com.what3words.javawrapper.response.SuggestionWithCoordinates
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class W3WApiDataSource(private val wrapper: What3WordsV3, private val context: Context) :
    W3WDataSource {

    override suspend fun getSuggestionByCoordinates(
        lat: Double,
        lng: Double,
        language: String
    ): Either<APIResponse.What3WordsError, SuggestionWithCoordinates> =
        suspendCoroutine { cont ->
            val c23wa =
                wrapper.convertTo3wa(Coordinates(lat, lng))
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

    override suspend fun getGrid(boundingBox: BoundingBox): Either<APIResponse.What3WordsError, List<Line>> =
        suspendCoroutine { cont ->
            val grid = wrapper.gridSection(
                boundingBox
            ).execute()
            if (grid.isSuccessful) {
                cont.resume(Either.Right(grid.lines))
            } else {
                cont.resume(Either.Left(grid.error))
            }
        }

    //TO BE FIXED ON THE WRAPPER
    override suspend fun getGeoJsonGrid(boundingBox: BoundingBox): Either<APIResponse.What3WordsError, String> =
        suspendCoroutine { cont ->
            val grid = wrapper.gridSectionGeoJson(
                boundingBox
            ).execute()
            if (grid.isSuccessful) {
                cont.resume(Either.Right(GsonBuilder().create().toJson(grid)))
            } else {
                cont.resume(Either.Left(grid.error))
            }
        }
}