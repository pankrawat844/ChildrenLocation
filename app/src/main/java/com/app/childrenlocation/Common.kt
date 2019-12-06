package com.app.childrenlocation

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import java.math.MathContext
import java.text.DateFormat
import java.util.*

object Common {
    val KEY_REQUEST_LOCATION_UPDATE="requesting_location"
    fun getLocationText(location: Location?):String
    {
        return if(location==null)
            "Unkonw Location"
        else
            location.latitude.toString()+"/"+location.longitude
    }

    fun getLocationTitle(context: Context):CharSequence?
    {
return String.format("Location Updated"+DateFormat.getDateInstance().format(Date()))

    }

    fun setReqestingLocationUpdates(context: Context, b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_REQUEST_LOCATION_UPDATE,b)
            .apply()
    }

    fun requestingLocationUpdates(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_REQUEST_LOCATION_UPDATE,false)
    }
}