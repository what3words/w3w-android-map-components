package com.what3words.components.compose.maps.models

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

/**
 * Represents a marker on a map, associated with a what3words address.
 *
 * @property words The what3words address (e.g., "filled.count.soap").
 * @property square The geographical boundary rectangle of the what3words square.
 * @property color The colors used for styling the marker.
 * @property center The center coordinates of the what3words square.
 * @property title Optional title to display when the marker is tapped.
 * @property snippet Optional snippet text to display when the marker is tapped.
 */
@Immutable
data class W3WMarker(
    val words: String,
    val square: W3WRectangle,
    val color: W3WMarkerColor,
    val center: W3WCoordinates,
    val title: String? = null,
    val snippet: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        W3WRectangle(
            southwest = W3WCoordinates(
                parcel.readDouble(),
                parcel.readDouble()
            ),
            northeast = W3WCoordinates(
                parcel.readDouble(),
                parcel.readDouble()
            )
        ),
        W3WMarkerColor(
            background = Color(parcel.readLong()),
            slash = Color(parcel.readLong())
        ),
        W3WCoordinates(
            parcel.readDouble(),
            parcel.readDouble()
        ),
        parcel.readString(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(words)
        dest.writeDouble(square.southwest.lat)
        dest.writeDouble(square.southwest.lng)
        dest.writeDouble(square.northeast.lat)
        dest.writeDouble(square.northeast.lng)
        dest.writeLong(color.background.value.toLong())
        dest.writeLong(color.slash.value.toLong())
        dest.writeDouble(center.lat)
        dest.writeDouble(center.lng)
        dest.writeString(title)
        dest.writeString(snippet)
    }

    companion object CREATOR : Parcelable.Creator<W3WMarker> {
        override fun createFromParcel(parcel: Parcel): W3WMarker {
            return W3WMarker(parcel)
        }

        override fun newArray(size: Int): Array<W3WMarker?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Defines the color scheme for a W3WMarker.
 *
 * @property background The background color of the marker.
 * @property slash The color of the slash in the marker.
 * @property id A unique identifier generated from the combination of background and slash colors.
 */
@Immutable
data class W3WMarkerColor(
    val background: Color,
    val slash: Color
) {
    val id: Long
        get() {
            val backgroundLong = background.toArgb().toLong() and 0xFFFFFFFFL
            val slashLong = slash.toArgb().toLong() and 0xFFFFFFFFL
            return (backgroundLong shl 32) or slashLong
        }
}