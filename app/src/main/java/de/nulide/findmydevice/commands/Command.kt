package de.nulide.findmydevice.commands

import android.content.Context
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.services.FmdJobService
import de.nulide.findmydevice.transports.Transport


abstract class Command(val context: Context) {
    companion object {
        private val TAG = this::class.simpleName
    }

    open val settings: Settings =
        JSONFactory.convertJSONSettings(IO.read(JSONMap::class.java, IO.settingsFileName))

    abstract val keyword: String
    abstract val usage: String

    @get:StringRes
    abstract val shortDescription: Int

    @get:StringRes
    abstract val longDescription: Int?

    abstract val requiredPermissions: List<Permission>
    open val optionalPermissions: List<Permission> = emptyList()

    fun missingRequiredPermissions(): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    @CallSuper
    open fun <T> execute(
        args: List<String>,
        transport: Transport<T>,
        job: FmdJobService?,
    ) {
        val missing = missingRequiredPermissions()
        if (missing.isNotEmpty()) {
            val msg = context.getString(
                R.string.cmd_missing_permissions,
                args.joinToString(" "),
                missing.joinToString(", ")
            )
            Log.w(TAG, msg)
            transport.send(context, msg)
            job?.jobFinished()
            return
        }
        // continue executing command
        // (this should be done in the concrete classes that override this function)
    }
}
