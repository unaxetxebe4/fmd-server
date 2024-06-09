package de.nulide.findmydevice.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.nulide.findmydevice.R


class LocationPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_location_name

    fun isForegroundGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    fun isBackgroundGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PERMISSION_GRANTED
        } else true
    }

    override fun isGranted(context: Context): Boolean {
        return isForegroundGranted(context) && isBackgroundGranted(context)
    }

    val REQUEST_CODE = 8050

    override fun request(activity: Activity) {
        val array = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(activity, array, REQUEST_CODE)
    }
}
