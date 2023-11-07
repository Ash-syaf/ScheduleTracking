package com.example.scheduletracking

import android.os.Parcel
import android.os.Parcelable


class BusStopData(val title: String, val logo: Int, val latitude: Double, val longitude: Double) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeInt(logo)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BusStopData> {
        override fun createFromParcel(parcel: Parcel): BusStopData {
            return BusStopData(parcel)
        }

        override fun newArray(size: Int): Array<BusStopData?> {
            return arrayOfNulls(size)
        }
    }
}