package com.what3words.map.components.compose

import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.domain.W3WSuggestion
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WDistance
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage

class DummyUtils {
    companion object {
        val dummyCoordinates = W3WCoordinates(51.520847, -0.195521)

        val dummySquare = W3WRectangle(
            southwest = W3WCoordinates(
                51.520847,
                -0.195521
            ), northeast = W3WCoordinates(51.520847, -0.195521)
        )

        val dummyW3WAddress =
            W3WAddress(
                words = "filled.count.soap",
                center = dummyCoordinates,
                square = dummySquare,
                language = W3WProprietaryLanguage("en", null, null, null),
                country = W3WCountry("GB"),
                nearestPlace = "London"
            )

        val dummyW3WSuggestion = W3WSuggestion(dummyW3WAddress, rank = 1, distanceToFocus = W3WDistance(1.0))
    }
}