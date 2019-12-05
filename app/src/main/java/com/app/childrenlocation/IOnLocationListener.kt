package com.app.childrenlocation

import com.google.android.gms.maps.model.LatLng

interface IOnLocationListener {
    fun  OnLocationLoadSuccess(latLng: List<MyLatLng>)
    fun OnLocationLoadFailour(message:String)
}