package de.nulide.findmydevice.net

import android.content.Context
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import de.nulide.findmydevice.utils.PatchedVolley
import de.nulide.findmydevice.utils.SingletonHolder


class OpenCelliDRepository private constructor(private val openCelliDSpec: OpenCelliDSpec) {

    companion object :
        SingletonHolder<OpenCelliDRepository, OpenCelliDSpec>(::OpenCelliDRepository) {
        val TAG = OpenCelliDRepository::class.simpleName
    }

    private val requestQueue: RequestQueue = PatchedVolley.newRequestQueue(openCelliDSpec.context)

    fun getCellLocation(
        operator: String,
        location: GsmCellLocation,
        apiAccessToken: String,
        onSuccess: (OpenCelliDSuccess) -> Unit,
        onError: (OpenCelliDError) -> Unit,
    ) {
        if (apiAccessToken.isEmpty() || operator.length <= 3) {
            val error = "API Access Token empty or Operator too short. operator=$operator"
            Log.w(TAG, error)
            onError(OpenCelliDError(error, "no URL"))
            return
        }

        val mcc: Int = operator.substring(0, 3).toInt()
        val mnc: Int = operator.substring(3).toInt()
        val lac = location.lac
        val cid = location.cid

        val url =
            "https://opencellid.org/cell/get?key=$apiAccessToken&mcc=$mcc&mnc=$mnc&lac=$lac&cellid=$cid&format=json"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.has("lat") && response.has("lon")) {
                    val lat = response.getString("lat")
                    val lon = response.getString("lon")
                    onSuccess(OpenCelliDSuccess(lat, lon, url))
                } else {
                    Log.w(TAG, "Missing lat or lon in response")
                    onError(
                        OpenCelliDError("Missing lat or lon in response", url)
                    )
                }
            },
            { error ->
                Log.w(TAG, "Request failed: ${error.message}")
                onError(
                    OpenCelliDError(error.message ?: "", url)
                )
            },
        )
        requestQueue.add(request)
    }
}

class OpenCelliDSpec(val context: Context)

data class OpenCelliDSuccess(val lat: String, val lon: String, val url: String)
data class OpenCelliDError(val error: String, val url: String)
