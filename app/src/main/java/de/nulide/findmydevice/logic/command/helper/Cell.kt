package de.nulide.findmydevice.logic.command.helper

import android.util.Log
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepoSpec
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.logic.ComponentHandler
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec
import de.nulide.findmydevice.utils.CellParameters


class Cell {

    companion object {
        private val TAG = Cell::class.simpleName

        fun sendGSMCellLocation(ch: ComponentHandler) {
            val context = ch.context

            val settings = SettingsRepository.getInstance(SettingsRepoSpec(context)).settings
            val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String
            if (apiAccessToken.isEmpty()) {
                Log.i(TAG, "Cannot send cell location: Missing API Access Token")
                return
            }

            val paras = CellParameters.queryCellParametersFromTelephonyManager(context)
            if (paras == null) {
                Log.i(TAG, "No cell location found")
                ch.sender.sendNow(context.getString(R.string.OpenCellId_test_no_connection))
                return
            }
            ch.sender.sendNow(paras.prettyPrint())

            val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
            repo.getCellLocation(
                paras, apiAccessToken,
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
