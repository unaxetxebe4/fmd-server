package de.nulide.findmydevice.commands

import android.content.Context
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.permissions.WriteSecureSettingsPermission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.SecureSettings


class GpsCommand(context: Context) : Command(context) {

    override val keyword = "gps"
    override val usage = "gps [on | off]"

    @get:StringRes
    override val shortDescription = R.string.cmd_gps_description_short

    override val longDescription = null

    override val requiredPermissions = listOf(WriteSecureSettingsPermission())

    override fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
        job: FmdJobService?,
    ) {
        super.execute(args, transport, job)

        if (args.contains("on")) {
            SecureSettings.turnGPS(context, true)
            settings.set(Settings.SET_GPS_STATE, 1)
            transport.send(context, context.getString(R.string.cmd_gps_response_on))
        } else if (args.contains("off")) {
            SecureSettings.turnGPS(context, false)
            settings.set(Settings.SET_GPS_STATE, 0)
            transport.send(context, context.getString(R.string.cmd_gps_response_off))
        }
        job?.jobFinished()
    }
}