package de.nulide.findmydevice.commands

import android.content.Context
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport


class HelpCommand(
    private val availableCommands: List<Command>,
    context: Context,
) : Command(context) {

    override val keyword = "help"
    override val usage = "help"

    @get:StringRes
    override val shortDescription = R.string.cmd_help_description_short

    override val longDescription = null

    override val requiredPermissions = emptyList<Permission>()

    override fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
        job: FmdJobService?,
    ) {
        super.execute(args, transport, job)

        val reply = StringBuilder()
        reply.appendLine(context.getString(R.string.MH_Title_Help))
        reply.appendLine()
        for (cmd in availableCommands) {
            reply.appendLine("${cmd.keyword} - ${cmd.shortDescription}")
        }
        transport.send(context, reply.toString())
        job?.jobFinished()
    }
}
