package de.nulide.findmydevice.logic.command.helper

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import android.text.TextUtils
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.logic.ComponentHandler
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec

class Cell {

    companion object {
        fun sendGSMCellLocation(ch: ComponentHandler) {
            val context = ch.context

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val operator = tm.networkOperator
            // TODO: Migrate to CellInfo (GsmCellLocation is deprecated)
            @SuppressLint("MissingPermission") val location = tm.cellLocation as GsmCellLocation

            val msg: StringBuilder = StringBuilder(ch.context.getString(R.string.GPS_GSM_Data))
                .append("\n")
                .append("cid: ").append(location.cid)
                .append("\nlac: ").append(location.lac)
                .append("\n")
            if (!TextUtils.isEmpty(operator)) {
                val mcc = operator.substring(0, 3).toInt()
                val mnc = operator.substring(3).toInt()
                msg.append("mcc: ").append(mcc).append("\nmnc: ").append(mnc)
            }
            ch.sender.sendNow(msg.toString())

            val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
            val apiAccessToken = ch.settings.get(Settings.SET_OPENCELLID_API_KEY) as String

            repo.getCellLocation(
                operator, location, apiAccessToken,
                onSuccess = {
                    ch.locationHandler.newLocation("OpenCelliD", it.lat, it.lon)
                },
                onError = {
                    val string = ch.context.getString(R.string.JSON_RL_Error)
                    ch.sender.sendNow(string + it.url)
                },
            )
        }
    }
}
