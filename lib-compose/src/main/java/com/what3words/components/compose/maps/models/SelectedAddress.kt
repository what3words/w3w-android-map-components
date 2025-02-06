package com.what3words.components.compose.maps.models

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.what3words.components.compose.maps.W3WMapManager
import com.what3words.core.types.domain.W3WAddress
import com.what3words.core.types.domain.W3WCountry
import com.what3words.core.types.geometry.W3WCoordinates
import com.what3words.core.types.geometry.W3WRectangle
import com.what3words.core.types.language.W3WProprietaryLanguage
import com.what3words.core.types.language.W3WRFC5646Language

/**
 * This class wraps [W3WAddress] to be parcelable in [W3WMapManager.Saver].
 */
@Immutable
data class SelectedAddress(
    val address: W3WAddress
) : Parcelable {

    constructor(parcel: Parcel) : this(
        W3WAddress(
            words = parcel.readString()!!,
            center = W3WCoordinates(
                parcel.readDouble(),
                parcel.readDouble()
            ),
            square = W3WRectangle(
                southwest = W3WCoordinates(
                    parcel.readDouble(),
                    parcel.readDouble()
                ),
                northeast = W3WCoordinates(
                    parcel.readDouble(),
                    parcel.readDouble()
                )
            ),
            language = if (parcel.readInt() == 0) {
                W3WRFC5646Language.entries.find { it.code == parcel.readString()!! }!!
            } else {
                W3WProprietaryLanguage(
                    code = parcel.readString()!!,
                    locale = parcel.readString(),
                    name = parcel.readString(),
                    nativeName = parcel.readString()
                )
            },
            country = W3WCountry(
                twoLetterCode = parcel.readString()!!
            ),
            nearestPlace = parcel.readString()!!
        )
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(address.words)
        dest.writeDouble(address.center!!.lat)
        dest.writeDouble(address.center!!.lng)
        dest.writeDouble(address.square!!.southwest.lat)
        dest.writeDouble(address.square!!.southwest.lng)
        dest.writeDouble(address.square!!.northeast.lat)
        dest.writeDouble(address.square!!.northeast.lat)
        if (address.language is W3WRFC5646Language) {
            dest.writeInt(0)
            dest.writeString((address.language as W3WRFC5646Language).code)
        } else if (address.language is W3WProprietaryLanguage) {
            dest.writeInt(1)
            dest.writeString((address.language as W3WProprietaryLanguage).code)
            dest.writeString((address.language as W3WProprietaryLanguage).locale)
            dest.writeString((address.language as W3WProprietaryLanguage).name)
            dest.writeString((address.language as W3WProprietaryLanguage).nativeName)
        }
        dest.writeString(address.country.twoLetterCode)
        dest.writeString(address.nearestPlace)
    }

    companion object CREATOR : Parcelable.Creator<SelectedAddress> {
        override fun createFromParcel(parcel: Parcel): SelectedAddress {
            return SelectedAddress(parcel)
        }

        override fun newArray(size: Int): Array<SelectedAddress?> {
            return arrayOfNulls(size)
        }
    }
}