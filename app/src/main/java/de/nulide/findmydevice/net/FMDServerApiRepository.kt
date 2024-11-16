package de.nulide.findmydevice.net

import android.content.Context
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.PatchedVolley
import de.nulide.findmydevice.utils.SingletonHolder
import de.nulide.findmydevice.utils.log
import org.json.JSONException
import org.json.JSONObject
import java.util.Date


data class FMDServerApiRepoSpec(
    val context: Context,
)

/**
 * All network requests run on a background thread. This is handled by Volley.
 */
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
    private var baseUrl = ""
    private val queue: RequestQueue = PatchedVolley.newRequestQueue(spec.context)
    private val settingsRepo = SettingsRepository.getInstance(context)

    init {
        loadBaseUrl()
    }

    /**
     * Reload the base URL from settings and cache it in a local field.
     * This should be called every time where the settings could have changed.
     */
    private fun loadBaseUrl() {
        val tempBaseUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        // ensure the base URL doesn't end in /
        if (tempBaseUrl.endsWith("/")) {
            settingsRepo.set(
                Settings.SET_FMDSERVER_URL,
                tempBaseUrl.trim('/')
            )
        }
        baseUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
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
        loadBaseUrl()
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
                    settingsRepo.set(Settings.SET_FMDSERVER_ID, response["DeviceId"])
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

    // TODO: cache access tokens for 5 mins (or whatever their validity period) to avoid unnecessary renewals

    fun getAccessToken(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        getAccessToken(
            settingsRepo.get(Settings.SET_FMDSERVER_ID) as String,
            settingsRepo.get(Settings.SET_FMD_CRYPT_HPW) as String,
            onResponse,
            onError,
        )
    }

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

    /**
     * This MUST be wrapped in a Thread() because it does password hashing.
     *
     * TODO: handled this internally in the repo.
     */
    fun login(
        userId: String,
        password: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        loadBaseUrl()

        getSalt(userId, onError = onError, onResponse = { salt ->
            val hashedPW = CypherUtils.hashPasswordForLogin(password, salt)
            getAccessToken(userId, hashedPW, onError = onError, onResponse = { accessToken ->
                getPrivateKey(accessToken, onError = onError, onResponse = { privateKey ->
                    getPublicKey(accessToken, onError = onError, onResponse = { publicKey ->
                        settingsRepo.apply {
                            set(Settings.SET_FMD_CRYPT_HPW, hashedPW)
                            set(Settings.SET_FMDSERVER_ID, userId)
                            set(Settings.SET_FMD_CRYPT_PUBKEY, publicKey)
                            set(Settings.SET_FMD_CRYPT_PRIVKEY, privateKey)
                        }
                        onResponse.onResponse(Unit)
                    })
                })
            })
        })
    }

    fun unregister(
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("Data", "")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                // XXX: This should be a dedicated /deleteDevice endpoint
                Method.POST, baseUrl + URL_DEVICE, jsonObject,
                { _ -> onResponse.onResponse(Unit) },
                { error ->
                    // FIXME: The server returns an empty body which cannot be parsed to JSON.
                    // The best solution would be for the access token to be passed as a header rather then a body
                    // FIXME: also the server does not explicitly return a 200, so e.g. nginx closes the connection with 499
                    if (error.cause is JSONException || error.networkResponse.statusCode == 499) {
                        // request was actually successful, just deserialising failed
                        // only clear if request is successful
                        settingsRepo.removeServerAccount()
                        onResponse.onResponse(Unit)
                    } else {
                        onError.onErrorResponse(error)
                    }
                },
            )
            queue.add(request)
        })
    }

    fun registerPushEndpoint(
        endpoint: String,
        onError: Response.ErrorListener,
    ) {
        context.log().i(TAG, "Registering push endpoint $endpoint")
        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("Data", endpoint)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                Method.PUT, baseUrl + URL_PUSH, jsonObject,
                { _ -> },
                { error ->
                    // FIXME: The server returns an empty body which cannot be parsed to JSON.
                    // The best solution would be for the access token to be passed as a header rather then a body
                    if (error.cause is JSONException) {
                        // request was actually successful, just deserialising failed
                    } else {
                        onError.onErrorResponse(error)
                    }
                }
            )
            queue.add(request)
        })
    }

    fun changePassword(
        newHashedPW: String,
        newPrivKey: String,
        onResponse: Response.Listener<Unit>,
        onError: Response.ErrorListener,
    ) {
        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("hashedPassword", newHashedPW)
                jsonObject.put("privkey", newPrivKey)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                Method.POST, baseUrl + URL_PASSWORD, jsonObject,
                { response ->
                    if (response.has("Data")) {
                        settingsRepo.set(Settings.SET_FMD_CRYPT_PRIVKEY, newPrivKey)
                        settingsRepo.set(Settings.SET_FMD_CRYPT_HPW, newHashedPW)
                        onResponse.onResponse(Unit)
                    } else {
                        onError.onErrorResponse(VolleyError("change password response has no Data field"))
                    }
                },
                onError,
            )
            queue.add(request)
        })
    }

    fun getCommand(
        onResponse: Response.Listener<String>,
        onError: Response.ErrorListener,
    ) {
        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("Data", "")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                // XXX: This should be GET (or POST-as-GET) instead of PUT
                Method.PUT, baseUrl + URL_COMMAND, jsonObject,
                { response ->
                    try {
                        val command = response["Data"] as String
                        onResponse.onResponse(command)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        onError.onErrorResponse(VolleyError("get command response has no Data field"))
                    }
                },
                onError,
            )
            queue.add(request)
        })
    }

    /**
     * This MUST be wrapped in a Thread() because it does async crypto.
     *
     * TODO: handled this internally in the repo.
     */
    fun sendPicture(
        picture: String,
    ) {
        val publicKey = settingsRepo.getKeys()?.publicKey
        if (publicKey == null) {
            context.log().e(TAG, "Public key was null")
            return
        }
        val msgBytes = CypherUtils.encryptWithKey(publicKey, picture)
        val msg = CypherUtils.encodeBase64(msgBytes)

        val onError = { error: VolleyError -> error.printStackTrace() }

        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("Data", msg)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                Method.POST, baseUrl + URL_PICTURE, jsonObject,
                { _ -> },
                onError,
            )
            queue.add(request)
        })
    }

    /**
     * This MUST be wrapped in a Thread() because it does async crypto.
     *
     * TODO: handled this internally in the repo.
     */
    fun sendLocation(
        provider: String, lat: String, lon: String, batLevel: String, timeInMillis: Long
    ) {
        // Prepare payload
        val publicKey = settingsRepo.getKeys()?.publicKey
        if (publicKey == null) {
            context.log().e(TAG, "Public key was null")
            return
        }

        val locationDataObject = JSONObject()
        try {
            locationDataObject.put("provider", provider)
            locationDataObject.put("date", timeInMillis)
            locationDataObject.put("bat", batLevel)
            locationDataObject.put("lon", lon)
            locationDataObject.put("lat", lat)
            locationDataObject.put("time", Date(timeInMillis).toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val jsonSerialised = locationDataObject.toString()
        val encryptedLocationBytes = CypherUtils.encryptWithKey(publicKey, jsonSerialised)
        val encryptedLocation = CypherUtils.encodeBase64(encryptedLocationBytes)

        // Send payload
        val onError = { error: VolleyError -> error.printStackTrace() }

        getAccessToken(onError = onError, onResponse = { accessToken ->
            val jsonObject = JSONObject()
            try {
                jsonObject.put("IDT", accessToken)
                jsonObject.put("Data", encryptedLocation)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val request = JsonObjectRequest(
                Method.POST, baseUrl + URL_LOCATION, jsonObject,
                { _ -> },
                onError,
            )
            queue.add(request)
        })
    }

}
