package com.jijith.alexa.service.handlers

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.amazon.aace.location.LocationProvider
import timber.log.Timber
import java.io.IOException
import java.util.*

class LocationProviderHandler(private var context: Context) : LocationProvider(), LocationListener {

    /// A string to identify log entries originating from this file
    private val TAG = "LocationProvider"

    /// The minimum time interval in milliseconds between updates from the location provider
    private val MIN_REFRESH_TIME = 60000 // 1 minute

    /// The minimum distance in meters between updates from the location provider
    private val MIN_REFRESH_DISTANCE = 0 // 0 meters

    /// The time interval in milliseconds for which a new location update will always be accepted
    /// over the current estimate
    private val LOCATION_UPDATE_TIMEOUT = 120000 // 2 minutes

    /// The view containing the mock location input UI elements
    private var mAddressEntry: View? = null

    /// The mock location text entry field
    private var mAddressText: EditText? = null

    /// The latitude/longitude location display
    private var mLatLongText: TextView? = null

    /// The object providing access to system location services
    private var mLocationManager: LocationManager? = null

    /// The object handling geocoding for mock location
    private var mGeocoder: Geocoder? = null

    /// The current physical location best estimate
    private var mCurrentLocation: Location? = null

    /// The most recently set mock location
    private var mMockLocation: Location? = null

    /// Whether mock location is in use
    private var mMockLocationEnabled = false

    /// Available providers
    private var mAvailableProviders: HashSet<String>? = null

    init {
        // Initialize the mock and physical location providers
        mGeocoder = Geocoder(context)
        mLocationManager = context.applicationContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Initialize set of available providers
        mAvailableProviders = HashSet()
        val availableProviders =
            mLocationManager!!.allProviders
        for (provider in availableProviders) {
            mAvailableProviders!!.add(provider)
        }
        requestLocationUpdates(LocationManager.NETWORK_PROVIDER)
        requestLocationUpdates(LocationManager.GPS_PROVIDER)

        // Retrieve an initial location estimate cached by the location providers
        updateCurrentLocation(LocationManager.NETWORK_PROVIDER)
        updateCurrentLocation(LocationManager.GPS_PROVIDER)

        // Set an initial default mock location
        mMockLocation = Location("")
        mMockLocation!!.latitude = 0.0
        mMockLocation!!.longitude = 0.0
        mMockLocation!!.altitude = 0.0
    }

    override fun getLocation(): com.amazon.aace.location.Location? {
        if (mMockLocationEnabled) {
            val mockLatitude = mMockLocation!!.latitude
            val mockLongitude = mMockLocation!!.longitude

            // prevents coordinate from being (0,0)
            return if (mockLatitude == 0.0 && mockLongitude == 0.0) {
                com.amazon.aace.location.Location(
                    com.amazon.aace.location.Location.UNDEFINED,
                    com.amazon.aace.location.Location.UNDEFINED
                )
            } else com.amazon.aace.location.Location(
                mockLatitude,
                mockLongitude,
                mMockLocation!!.altitude
            )
        }
        if (mCurrentLocation == null) {
            Timber.v(
                "No location found. Geolocation context will not be sent. " +
                        "Defaulting to AVS cloud. "
            )
            return com.amazon.aace.location.Location(
                com.amazon.aace.location.Location.UNDEFINED,
                com.amazon.aace.location.Location.UNDEFINED
            )
        }
        val latitude = mCurrentLocation!!.latitude
        val longitude = mCurrentLocation!!.longitude

        // prevents coordinate from being (0,0)
        return if (latitude == 0.0 && longitude == 0.0) {
            com.amazon.aace.location.Location(
                com.amazon.aace.location.Location.UNDEFINED,
                com.amazon.aace.location.Location.UNDEFINED
            )
        } else com.amazon.aace.location.Location(
            latitude,
            longitude,
            mCurrentLocation!!.altitude
        )

//        return null;
    }

    override fun getCountry(): String? {
        // Get device country from a platform specific method/service.
        // As an example "US" is set here by default.
        return "US"
    }

    override fun onLocationChanged(location: Location) {
        if (!mMockLocationEnabled) {
            updateLocation(location)
        }
    }

    override fun onProviderDisabled(provider: String?) {
        Timber.v(String.format("provider disabled: %s", provider))
        mCurrentLocation = null
    }

    override fun onProviderEnabled(provider: String) {
        Timber.v(String.format("provider enabled: %s", provider))
        requestLocationUpdates(provider)
        updateCurrentLocation(provider)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Timber.v(
            String.format("provider status changed: %s, status: %s", provider, status)
        )
    }

    /**
     * Register for location updates from the named provider
     *
     * @param provider The name of the provider with which to register
     */
    private fun requestLocationUpdates(provider: String) {
        Timber.v(
            String.format("Requesting location updates using %s", provider)
        )
        if (mAvailableProviders!!.contains(provider)) {
            try {
                mLocationManager!!.requestLocationUpdates(
                    provider,
                    MIN_REFRESH_TIME.toLong(),
                    MIN_REFRESH_DISTANCE.toFloat(),
                    this
                )
            } catch (e: SecurityException) {
                Timber.e(e.message)
            } catch (illegalError: IllegalArgumentException) {
                Timber.e(illegalError.message)
            }
        } else {
            Timber.v(String.format("Provider %s is not available", provider))
        }
    }

    /**
     * Updates the current location estimate with the last known location fix obtained from the
     * given provider. Note the location may be out-of-date.
     *
     * @param provider The provider from which to get the last known location
     */
    private fun updateCurrentLocation(provider: String) {
        try {
            // Request location from the provider only if it's enabled
            if (!mLocationManager!!.isProviderEnabled(provider)) {
                Timber.i(
                    String.format(
                        "Attempted to get last known location but %s location provider is disabled",
                        provider
                    )
                )
            } else {
                // Get the last known fix from the provider and update the current location estimate
                val lastKnownLocation =
                    mLocationManager!!.getLastKnownLocation(provider)
                if (lastKnownLocation != null) {
                    updateLocation(lastKnownLocation)
                } else {
                    Timber.i(
                        String.format("last %s location not found", provider)
                    )
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e.message)
        }
    }

    /**
     * Update the current location best estimate using the provided location. If the provided
     * location is not better than the current estimate, the current estimate will not be updated
     * unless it has expired.
     *
     * @param location The location to set as the current estimate
     */
    private fun updateLocation(location: Location) {
        if (mCurrentLocation != null) {
            // Only update if accuracy is equivalent or better or 2 mins since last update
            if (location.accuracy <= mCurrentLocation!!.accuracy ||
                System.currentTimeMillis() - mCurrentLocation!!.time > LOCATION_UPDATE_TIMEOUT
            ) {
                Timber.v("location updated: " + locationToString(location))
                mCurrentLocation = location
            }
        } else {
            Timber.v("location updated: " + locationToString(location))
            mCurrentLocation = location
        }
    }

    /**
     * Enables or disables use of mock location. When enabled, the most recently set mock location
     * will be sent to the Engine until mock location is disabled.
     *
     * @param enable Whether mock location should be enabled
     */
    private fun enableMockLocation(enable: Boolean) {
        mMockLocationEnabled = enable
        if (enable) {
            Timber.i("Using mock location")
        } else {
            Timber.i("Using device location")
            if (mCurrentLocation != null) {
            }
        }
    }

    /**
     * Sets the current mock location. Uses geocoding to construct a location from the
     * provided string descriptor to send to the Engine. The provided location descriptor may
     * represent a place name, an address, an airport code, etc.
     *
     * @param location A string description of the location to set
     */
    private fun setMockLocation(location: String) {
        if (mMockLocationEnabled) {
            try {
                val addressList =
                    mGeocoder!!.getFromLocationName(location, 1)
                if (addressList == null || addressList.size == 0) {
                    Timber.w(
                        String.format(
                            "No match found by the geocoder for the "
                                    + "location \"%s\". " + "Location not updated.", location
                        )
                    )
                } else {
                    val address = addressList[0]
                    mMockLocation = Location("")
                    mMockLocation!!.latitude = address.latitude
                    mMockLocation!!.longitude = address.longitude
                    mMockLocation!!.altitude = 0.0
                    mMockLocation!!.accuracy = 0f
                    mMockLocation!!.time = System.currentTimeMillis()
                    Timber.i(
                        String.format(
                            "Location set to \"%s\" (%.3f, %.3f)",
                            location, address.latitude, address.longitude
                        )
                    )
                }
            } catch (e: IOException) {
                Timber.w(
                    String.format("Unable to geocode the provided location \"%s\"", location)
                )
            }
        }
    }

    /**
     * Produces a string representation of a Location for logging
     *
     * @param location The location to represent as a string
     * @return The string representation of the location
     */
    private fun locationToString(location: Location): String {
        return String.format(
            "provider: %s, latitude: %s, longitude: %s, altitude: %s, accuracy: %s",
            location.provider,
            location.latitude,
            location.longitude,
            location.altitude,
            location.accuracy
        )
    }
}