package de.nulide.findmydevice.transports

import android.content.Context
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.locationproviders.LocationProvider
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.utils.Utils


// Order matters for the home screen
fun availableTransports(context: Context): List<Transport<*>> = listOf(
    SmsTransport(context, "42"),
    NotificationReplyTransport(null),
    FmdServerTransport(context),
    InAppTransport(context),
)


abstract class Transport<DestinationType>(
    private val destination: DestinationType
) {
    companion object {
        private val TAG = Transport::class.simpleName
    }

    @get:DrawableRes
    abstract val icon: Int

    @get:StringRes
    abstract val title: Int

    @get:StringRes
    abstract val description: Int

    @get:StringRes
    open val descriptionAuth: Int? = null

    @get:StringRes
    open val descriptionNote: Int? = null

    abstract val requiredPermissions: List<Permission>

    open val actions: List<TransportAction> = emptyList()

    fun missingRequiredPermissions(context: Context): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    abstract fun getDestinationString(): String

    @CallSuper
    open fun send(context: Context, msg: String) {
        val missing = missingRequiredPermissions(context)
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Cannot send message: missing permissions ${missing.joinToString(", ")}")
            return
        }
        // continue sending message
        // (this should be done in the concrete classes that override this function)
    }

    open fun sendNewLocation(
        context: Context,
        provider: String,
        lat: String,
        lon: String,
        timeMillis: Long,
    ) {
        val batteryLevel = Utils.getBatteryLevel(context)
        val msg = LocationProvider.buildLocationString(provider, lat, lon, batteryLevel, timeMillis)
        send(context, msg)
    }
}
