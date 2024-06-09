package de.nulide.findmydevice.transports

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import de.nulide.findmydevice.net.FMDServerApiRepoSpec
import de.nulide.findmydevice.net.FMDServerApiRepository
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.utils.Utils


class FmdServerTransport(context: Context) : Transport<Unit>(Unit) {
    companion object {
        private val TAG = this::class.simpleName
    }

    private val repo = FMDServerApiRepository.getInstance(FMDServerApiRepoSpec(context))

    override val requiredPermissions = emptyList<Permission>()

    @SuppressLint("MissingSuperCall")
    override fun send(context: Context, msg: String) {
        //super.send(context, msg, destination)

        Log.w(
            TAG,
            "Not sending message. Reason: generic send() is not implemented for FmdServerTransport"
        )
    }

    override fun sendNewLocation(
        context: Context,
        provider: String,
        lat: String,
        lon: String,
        timeMillis: Long,
    ) {
        // no call to super(), we need to completely replace this for FMD Server

        val batteryLevel = Utils.getBatteryLevel(context)
        repo.sendLocation(provider, lat, lon, batteryLevel, timeMillis)
    }
}
