package com.what3words.components.compose.maps.models

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle

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