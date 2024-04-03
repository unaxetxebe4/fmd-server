package de.nulide.findmydevice.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.databinding.ActivityOpenCellIdBinding
import de.nulide.findmydevice.net.OpenCelliDRepository
import de.nulide.findmydevice.net.OpenCelliDSpec
import de.nulide.findmydevice.utils.CellParameters
import de.nulide.findmydevice.utils.Utils.Companion.getGeoURI
import de.nulide.findmydevice.utils.Utils.Companion.getOpenStreetMapLink
import de.nulide.findmydevice.utils.Utils.Companion.openUrl
import de.nulide.findmydevice.utils.Utils.Companion.pasteFromClipboard


class OpenCellIdActivity : AppCompatActivity(), TextWatcher {

    private lateinit var viewBinding: ActivityOpenCellIdBinding

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityOpenCellIdBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        settings =
            JSONFactory.convertJSONSettings(IO.read(JSONMap::class.java, IO.settingsFileName))
        val apiToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        viewBinding.editTextOpenCellIDAPIKey.setText(apiToken)
        viewBinding.editTextOpenCellIDAPIKey.addTextChangedListener(this)

        viewBinding.buttonPaste.setOnClickListener(::onPasteClicked)
        viewBinding.buttonOpenOpenCellIdWebsite.setOnClickListener(::onOpenWebsiteClicked)
        viewBinding.buttonTestOpenCellId.setOnClickListener(::onTestConnectionClicked)

        setupTestConnection(apiToken.isEmpty())
    }

    private fun setupTestConnection(isApiTokenEmpty: Boolean) {
        if (isApiTokenEmpty) {
            viewBinding.buttonTestOpenCellId.isEnabled = false
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.GONE
        } else {
            viewBinding.buttonTestOpenCellId.isEnabled = true
            viewBinding.textViewTestOpenCellIdResponse.text = ""
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.VISIBLE
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(edited: Editable) {
        if (edited === viewBinding.editTextOpenCellIDAPIKey.text) {
            val newToken = edited.toString().trim()
            settings.setNow(Settings.SET_OPENCELLID_API_KEY, newToken)
            setupTestConnection(newToken.isEmpty())
        }
    }

    private fun onPasteClicked(view: View) {
        viewBinding.editTextOpenCellIDAPIKey.setText(pasteFromClipboard(view.context))
    }

    private fun onOpenWebsiteClicked(view: View) {
        openUrl(view.context, "https://opencellid.org/")
    }

    private fun onTestConnectionClicked(view: View) {
        val context = view.context

        val paras = CellParameters.queryCellParametersFromTelephonyManager(context)
        if (paras == null) {
            Log.i(TAG, "No cell location found")
            viewBinding.textViewTestOpenCellIdResponse.text =
                context.getString(R.string.OpenCellId_test_no_connection)
            return
        }

        val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
        val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        repo.getCellLocation(
            paras, apiAccessToken,
            onSuccess = {
                val geoURI = getGeoURI(it.lat, it.lon)
                val osm = getOpenStreetMapLink(it.lat, it.lon)
                viewBinding.textViewTestOpenCellIdResponse.text =
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n${geoURI}\nOpenStreetMap: $osm"
            },
            onError = {
                viewBinding.textViewTestOpenCellIdResponse.text =
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n\nError: ${it.error}"
            },
        )
    }

    companion object {
        private val TAG = OpenCellIdActivity::class.simpleName
    }
}
