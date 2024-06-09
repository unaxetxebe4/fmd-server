package de.nulide.findmydevice.commands

import android.content.Context
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.permissions.DoNotDisturbAccessPermission
import de.nulide.findmydevice.permissions.OverlayPermission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.RingerUtils


class RingCommand(context: Context) : Command(context) {

    override val keyword = "ring"
    override val usage = "ring [long]"

    @get:StringRes
    override val shortDescription = R.string.cmd_ring_description_short

    override val longDescription = null

    // TODO(#145): Implement this without needing the overlay permission
    override val requiredPermissions = listOf(OverlayPermission())

    override val optionalPermissions = listOf(DoNotDisturbAccessPermission())

    override fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
        job: FmdJobService?,
    ) {
        super.execute(args, transport, job)

        val firstArg = args.getOrElse(0) { "" }

        var duration = 30
        if (firstArg == "long") {
            duration = 180
        } else if (firstArg.isNotEmpty()) {
            firstArg.toIntOrNull()?.let {
                duration = it
            }
        }
        RingerUtils.ring(context, duration)
        transport.send(context, context.getString(R.string.cmd_ring_response))
        job?.jobFinished()
    }
}
