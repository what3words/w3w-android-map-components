package com.what3words.components.maps.models

import com.what3words.javawrapper.request.BoundingBox
import com.what3words.javawrapper.request.Coordinates
import com.what3words.javawrapper.response.APIResponse
import com.what3words.javawrapper.response.GridSection
import com.what3words.javawrapper.response.SuggestionWithCoordinates

interface W3WDataSource {
    suspend fun getSuggestionByCoordinates(
        coordinates: Coordinates,
        language: String
    ): Either<APIResponse.What3WordsError, SuggestionWithCoordinates>

    suspend fun getSuggestionByWords(
        words: String
    ): Either<APIResponse.What3WordsError, SuggestionWithCoordinates>

    suspend fun getGrid(
        boundingBox: BoundingBox
    ): Either<APIResponse.What3WordsError, GridSection>

    suspend fun getGeoJsonGrid(
        boundingBox: BoundingBox
    ): Either<APIResponse.What3WordsError, String>
}
