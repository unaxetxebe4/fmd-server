package de.nulide.findmydevice.commands

import android.content.Context
import android.util.Log
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.Logger
import de.nulide.findmydevice.utils.Notifications


// Order matters for the home screen
fun availableCommands(context: Context): List<Command> {
    val commands = mutableListOf(
        CameraCommand(context),
        DeleteCommand(context),
        GpsCommand(context),
        // HelpCommand(context),
        LocateCommand(context),
        LockCommand(context),
        NoDisturbCommand(context),
        RingCommand(context),
        StatsCommand(context),
    )
    // FIXME: The HelpCommand does not know about itself
    commands.add(HelpCommand(commands, context))
    return commands
}

/**
 * CommandHandler is the entry point for taking a string,
 * mapping it to a Command, and executing the command.
 *
 * @param job
 * An optional FmdJobService that is running this command, and its JobParameters.
 * If this is non-null, the Command should call job.jobFinished() when it is done.
 * (This is like a callback.)
 */
class CommandHandler<T>(
    private val transport: Transport<T>,
    private val job: FmdJobService?,
) {

    /**
     * Executes commands of the form "triggerWord command options", e.g. "fmd locate cell"
     */
    fun execute(context: Context, rawCommand: String) {
        Logger.logSession(TAG, "Handling command: $rawCommand")
        Log.d(TAG, "Handling command: $rawCommand")

        val args = rawCommand.split(" ").toMutableList()
        val settings: Settings =
            JSONFactory.convertJSONSettings(IO.read(JSONMap::class.java, IO.settingsFileName))
        val fmdTriggerWord = settings.get(Settings.SET_FMD_COMMAND) as String

        if (args.isEmpty() || args[0].lowercase() != fmdTriggerWord.lowercase()) {
            return
        }

        showUsageNotification(context, rawCommand)

        if (args.size == 1) {
            // no argument ==> show help
            args.add("help")
        }

        // run the command
        for (cmd in availableCommands(context)) {
            if (args[1].lowercase() == cmd.keyword.lowercase()) {
                cmd.execute(args, transport, job)
                break
            }
        }
    }

    private fun showUsageNotification(context: Context, rawCommand: String) {
        Notifications.notify(
            context,
            context.getString(R.string.usage_notification_title),
            context.getString(R.string.usage_notification_text, rawCommand),
            Notifications.CHANNEL_USAGE
        )
    }

    companion object {
        val TAG = CommandHandler::class.simpleName

        // fmd <pin> locate
        @JvmStatic
        fun checkAndRemovePin(settings: Settings, msg: String): String? {
            val expectedHash = settings.get(Settings.SET_PIN) as String
            val parts = msg.split(" ")
            if (parts.size >= 2) {
                val pin = parts[1]
                if (CypherUtils.checkPasswordForFmdPin(expectedHash, pin)) {
                    return msg.replace(pin, "")
                }
            }
            return null
        }
    }
}
