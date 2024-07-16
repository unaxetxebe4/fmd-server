package de.nulide.findmydevice.locationproviders

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.permissions.LocationPermission
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.SecureSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.Calendar
import java.util.TimeZone


class GpsLocationProvider<T>(
    private val context: Context,
    private val transport: Transport<T>,
) : LocationProvider(), LocationListener {

    companion object {
        private val TAG = this::class.simpleName

        @JvmStatic
        fun isGpsOn(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var deferred: CompletableDeferred<Unit>? = null

    @SuppressLint("MissingPermission") // linter is not good enough to recognize the check
    override fun getAndSendLocation(): Deferred<Unit> {
        val def = CompletableDeferred<Unit>()
        deferred = def

        if (!LocationPermission().isGranted(context)) {
            Log.i(TAG, "Missing location permission, cannot get location")
            def.complete(Unit)
            return def
        }
        transport.send(context, context.getString(R.string.cmd_locate_response_gps_will_follow))

        Log.d(TAG, "Requesting location update from GPS")
        for (provider in locationManager.allProviders) {
            // we may be in a background thread due to being in a coroutine,
            // but this needs to be called on the main thread
            ContextCompat.getMainExecutor(context).execute {
                locationManager.requestLocationUpdates(provider, 1000, 0f, this)
            }
        }
        return def
    }

    override fun onLocationChanged(location: Location) {

        val provider = location.provider ?: "GPS"
        val lat = location.latitude.toString()
        val lon = location.longitude.toString()
        val timeMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

        storeLastKnownLocation(lat, lon, timeMillis)
        transport.sendNewLocation(context, provider, lat, lon, timeMillis)

        val settings: Settings =
            JSONFactory.convertJSONSettings(IO.read(JSONMap::class.java, IO.settingsFileName))

        if (settings[Settings.SET_GPS_STATE] as Int == 2) {
            SecureSettings.turnGPS(context, false)
            settings.set(Settings.SET_GPS_STATE, 0)
        }

        Log.d(TAG, "Location found by $provider")
        locationManager.removeUpdates(this)
        deferred?.complete(Unit)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
