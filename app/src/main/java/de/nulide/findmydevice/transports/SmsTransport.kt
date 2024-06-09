package de.nulide.findmydevice.transports

import android.content.Context
import android.telephony.SmsManager
import de.nulide.findmydevice.locationproviders.LocationProvider
import de.nulide.findmydevice.permissions.SmsPermission


class SmsTransport(
    private val destination: String
) : Transport<String>(destination) {

    override val requiredPermissions = listOf(SmsPermission())

    override fun getDestinationString(): String = destination

    override fun send(context: Context, msg: String) {
        super.send(context, msg)

        val smsManager = context.getSystemService(SmsManager::class.java)
        if (msg.length <= 160) {
            smsManager.sendTextMessage(destination, null, msg, null, null)
        } else {
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
        }
    }
}