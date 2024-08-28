package de.nulide.findmydevice.transports

import android.app.Activity
import androidx.annotation.StringRes


data class TransportAction(
    @StringRes
    val titleResourceId: Int,

    val run: (activity: Activity) -> Unit,
)