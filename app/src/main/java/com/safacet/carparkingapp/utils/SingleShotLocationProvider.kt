package com.safacet.carparkingapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import com.safacet.carparkingapp.R


class SingleShotLocationProvider {
    interface LocationCallback {
        fun onNewLocationAvailable(location: LatLng?)
    }

    // calls back to calling thread, note this is for low grain: if you want higher precision, swap the
    // contents of the else and if. Also be sure to check gps permission/settings are allowed.
    // call usually takes <10ms

    fun requestSingleUpdate(context: Context, callback: LocationCallback) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            when {
                isGPSEnabled -> {
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                        locationManager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER,
                            null,
                            context.mainExecutor
                        ) { location ->
                            if(location != null) {
                                callback.onNewLocationAvailable(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                )
                            } else {
                                callback.onNewLocationAvailable(null)
                            }
                        }
                    } else {
                        val criteria = Criteria()
                        criteria.accuracy = Criteria.ACCURACY_FINE
                        locationManager.requestSingleUpdate(criteria, object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                callback.onNewLocationAvailable(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                )
                            }

                            override fun onStatusChanged(
                                provider: String,
                                status: Int,
                                extras: Bundle
                            ) {
                            }

                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }, null)
                    }


                }
                isNetworkEnabled -> {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                        locationManager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER,
                            null,
                            context.mainExecutor
                        ) { location ->
                            if(location != null) {
                                callback.onNewLocationAvailable(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                )
                            } else {
                                callback.onNewLocationAvailable(null)
                            }
                        }
                    } else {
                        val criteria = Criteria()
                        criteria.accuracy = Criteria.ACCURACY_COARSE

                        locationManager.requestSingleUpdate(criteria, object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                callback.onNewLocationAvailable(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                )
                            }

                            override fun onStatusChanged(
                                provider: String,
                                status: Int,
                                extras: Bundle
                            ) {
                            }
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }, null)
                    }
                }
            }
        } else {
            Toast.makeText(context, context.resources.getString(R.string.unavailable_location_service), Toast.LENGTH_SHORT).show()
            callback.onNewLocationAvailable(null)
        }
    }
}

