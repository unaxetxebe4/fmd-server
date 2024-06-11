package de.nulide.findmydevice.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.permissions.DeviceAdminPermission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.utils.CypherUtils


class DeleteCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = this::class.simpleName
    }

    override val keyword = "delete"
    override val usage = "delete <pin>"

    @get:StringRes
    override val shortDescription = R.string.cmd_delete_description_short

    override val longDescription = R.string.cmd_delete_description_long

    override val requiredPermissions = listOf(DeviceAdminPermission())

    override fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
        job: FmdJobService?,
    ) {
        super.execute(args, transport, job)

        if (!(settings.get(Settings.SET_WIPE_ENABLED) as Boolean)) {
            val msg = context.getString(R.string.cmd_delete_response_disabled)
            Log.i(TAG, msg)
            transport.send(context, msg)
            job?.jobFinished()
            return
        }

        if (args.size < 3) {
            val triggerWord = settings.get(Settings.SET_FMD_COMMAND) as String
            val usage = "$triggerWord delete [pwd]"
            val msg = context.getString(R.string.cmd_delete_response_pwd_missing, usage)
            Log.i(TAG, msg)
            transport.send(context, msg)
            job?.jobFinished()
            return
        }
        // the args were previously split by space => restore the spaces
        val pwd = args.subList(3, args.size).joinToString(" ")

        if (CypherUtils.checkPasswordForFmdPin(settings.get(Settings.SET_PIN) as String, pwd)) {
            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            devicePolicyManager.wipeData(0)
            transport.send(context, context.getString(R.string.cmd_delete_response_success))
        } else {
            transport.send(context, context.getString(R.string.cmd_delete_response_pwd_wrong))
        }
        job?.jobFinished()
    }
}
