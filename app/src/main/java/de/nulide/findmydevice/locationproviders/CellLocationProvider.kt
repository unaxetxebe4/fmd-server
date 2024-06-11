package de.nulide.findmydevice.locationproviders

import android.content.Context
import android.util.Log
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepoSpec
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.CellParameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.Calendar
import java.util.TimeZone


class CellLocationProvider<T>(
    private val context: Context,
    private val transport: Transport<T>,
) : LocationProvider() {

    companion object {
        private val TAG = this::class.simpleName
    }

    override fun getAndSendLocation(): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()

        val settings = SettingsRepository.getInstance(SettingsRepoSpec(context)).settings
        val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String
        if (apiAccessToken.isEmpty()) {
            val msg = "Cannot send cell location: Missing API Token"
            Log.i(TAG, msg)
            transport.send(context, msg)
            deferred.complete(Unit)
            return deferred
        }

        val paras = CellParameters.queryCellParametersFromTelephonyManager(context)
        if (paras == null) {
            Log.i(TAG, "No cell location found")
            transport.send(context, context.getString(R.string.OpenCellId_test_no_connection))
            deferred.complete(Unit)
            return deferred
        }

        val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))

        Log.d(TAG, "Requesting location from OpenCelliD")
        repo.getCellLocation(
            paras, apiAccessToken,
            onSuccess = {
                Log.d(TAG, "Location found by OpenCelliD")
                val timeMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
                storeLastKnownLocation(it.lat, it.lon, timeMillis)
                transport.sendNewLocation(context, "OpenCelliD", it.lat, it.lon, timeMillis)
                deferred.complete(Unit)
            },
            onError = {
                Log.i(TAG, "Failed to get location from OpenCelliD")
                val msg =
                    context.getString(R.string.JSON_RL_Error) + it.url + "\n\n" + paras.prettyPrint()
                transport.send(context, msg)
                deferred.complete(Unit)
            },
        )
        return deferred
    }
}
