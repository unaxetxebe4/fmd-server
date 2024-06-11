package de.nulide.findmydevice.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.nulide.findmydevice.R


class ContactsPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_contacts_name

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PERMISSION_GRANTED
    }

    val REQUEST_CODE = 8030

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CODE
        )
    }
}
