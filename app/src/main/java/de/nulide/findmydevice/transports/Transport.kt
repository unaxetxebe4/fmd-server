package de.nulide.findmydevice.transports

import android.content.Context
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.locationproviders.LocationProvider
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.ui.helper.ConfigurationActivityInformation
import de.nulide.findmydevice.utils.Utils


// Order matters for the home screen
fun availableTransports(context: Context): List<Transport<*>> = listOf(
    SmsTransport("42"),
    NotificationReplyTransport(null),
    FmdServerTransport(context),
)


abstract class Transport<DestinationType>(
    private val destination: DestinationType
) {
    companion object {
        private val TAG = this::class.simpleName
    }

    @get:DrawableRes
    abstract val icon: Int

    @get:StringRes
    abstract val title: Int

    @get:StringRes
    abstract val description: Int

    @get:StringRes
    abstract val descriptionAuth: Int

    @get:StringRes
    abstract val descriptionNote: Int?

    abstract val requiredPermissions: List<Permission>

    open val configActivityInfo: ConfigurationActivityInformation? = null

    fun missingRequiredPermissions(context: Context): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    open fun getDestinationString(): String? = null

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
