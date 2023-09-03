package de.nulide.findmydevice.net

import android.content.Context
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.PatchedVolley
import de.nulide.findmydevice.utils.SingletonHolder
import org.json.JSONException
import org.json.JSONObject


data class FMDServerApiRepoSpec(
    val context: Context,
)

class FMDServerApiRepository private constructor(spec: FMDServerApiRepoSpec) {

    companion object :
        SingletonHolder<FMDServerApiRepository, FMDServerApiRepoSpec>(::FMDServerApiRepository) {

        val TAG = FMDServerApiRepository::class.simpleName

        const val MIN_REQUIRED_SERVER_VERSION = "0.4.0"

        private const val URL_ACCESS_TOKEN = "/requestAccess"
        private const val URL_COMMAND = "/command"
        private const val URL_LOCATION = "/location"
        private const val URL_PICTURE = "/picture"
        private const val URL_DEVICE = "/device"
        private const val URL_PUSH = "/push"
        private const val URL_SALT = "/salt"
        private const val URL_PRIVKEY = "/key"
        private const val URL_PUBKEY = "/pubKey"
        private const val URL_PASSWORD = "/password"
        private const val URL_VERSION = "/version"
    }

    private val context = spec.context
    private val baseUrl: String
    private val queue: RequestQueue = PatchedVolley.newRequestQueue(spec.context)
    private val settings: Settings

    init {
        // TODO: proper SettingsRepository that hides the IO magic
        IO.context = context
        settings = JSONFactory.convertJSONSettings(
            IO.read(JSONMap::class.java, IO.settingsFileName)
        )

        baseUrl = settings[Settings.SET_FMDSERVER_URL] as String
    }

    fun getServerVersion(
        customBaseUrl: String, // to allow querying other servers
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val request = StringRequest(
            Method.GET,
            customBaseUrl + URL_VERSION,
            onResponse,
            onError
        )
        queue.add(request)
    }

    fun registerAccount(
        privKey: String,
        pubKey: String,
        hashedPW: String,
        registrationToken: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("hashedPassword", hashedPW)
            jsonObject.put("pubkey", pubKey)
            jsonObject.put("privkey", privKey)
            jsonObject.put("registrationToken", registrationToken)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be POST instead of PUT
            Method.PUT, baseUrl + URL_DEVICE, jsonObject,
            { response: JSONObject ->
                try {
                    settings.setNow(Settings.SET_FMDSERVER_ID, response["DeviceId"])
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                onResponse.onResponse(Unit)
            },
            onError,
        )
        queue.add(request)
    }

    fun getSalt(
        userId: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", userId)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_SALT, jsonObject,
            { response ->
                try {
                    val salt = response["Data"] as String
                    onResponse.onResponse(salt)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError.onErrorResponse(VolleyError("Salt response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getAccessToken(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) =
        getAccessToken(
            settings.get(Settings.SET_FMDSERVER_ID) as String,
            settings.get(Settings.SET_FMD_CRYPT_HPW) as String,
            onResponse,
            onError,
        )

    fun getAccessToken(
        userId: String,
        hashedPW: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", userId)
            jsonObject.put("Data", hashedPW)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_ACCESS_TOKEN, jsonObject,
            { response ->
                try {
                    val accessToken = response["Data"] as String
                    onResponse.onResponse(accessToken)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError.onErrorResponse(VolleyError("Access Token response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getPrivateKey(
        accessToken: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_PRIVKEY, jsonObject,
            { response ->
                try {
                    val privateKey = response["Data"] as String
                    onResponse.onResponse(privateKey)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError.onErrorResponse(VolleyError("Private Key response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun getPublicKey(
        accessToken: String,
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("IDT", accessToken)
            jsonObject.put("Data", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = JsonObjectRequest(
            // XXX: This should be GET (or POST-as-GET) instead of PUT
            Method.PUT, baseUrl + URL_PUBKEY, jsonObject,
            { response ->
                try {
                    val publicKey = response["Data"] as String
                    onResponse.onResponse(publicKey)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    onError.onErrorResponse(VolleyError("Public Key response has no Data field"))
                }
            },
            onError,
        )
        queue.add(request)
    }

    fun login(
        userId: String,
        password: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        getSalt(userId, onError = onError, onResponse = { salt ->
            val hashedPW = CypherUtils.hashPasswordForLogin(password, salt)
            getAccessToken(userId, hashedPW, onError = onError, onResponse = { accessToken ->
                getPrivateKey(accessToken, onError = onError, onResponse = { privateKey ->
                    getPublicKey(accessToken, onError = onError, onResponse = { publicKey ->
                        settings.setNow(Settings.SET_FMD_CRYPT_HPW, hashedPW)
                        settings.setNow(Settings.SET_FMDSERVER_ID, userId)
                        settings.setNow(Settings.SET_FMD_CRYPT_PUBKEY, publicKey)
                        settings.setNow(Settings.SET_FMD_CRYPT_PRIVKEY, privateKey)
                        onResponse.onResponse(Unit)
                    })
                })
            })
        })
    }

}
