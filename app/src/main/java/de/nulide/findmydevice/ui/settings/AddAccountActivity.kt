package de.nulide.findmydevice.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.android.volley.VolleyError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.FmdKeyPair
import de.nulide.findmydevice.data.RegistrationTokenRepository
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.FMDServerApiRepoSpec
import de.nulide.findmydevice.net.FMDServerApiRepository
import de.nulide.findmydevice.receiver.PushReceiver
import de.nulide.findmydevice.services.FMDServerLocationUploadService
import de.nulide.findmydevice.ui.FmdActivity
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.Utils.Companion.copyToClipboard
import de.nulide.findmydevice.utils.Utils.Companion.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import java.util.Calendar
import java.util.TimeZone

class AddAccountActivity : FmdActivity(), TextWatcher {
    private lateinit var editTextServerUrl: EditText
    private lateinit var textViewServerVersion: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var fmdServerRepo: FMDServerApiRepository
    private lateinit var registrationTokensRepo: RegistrationTokenRepository

    private var loadingDialog: AlertDialog? = null

    private var lastTextChangedMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        settingsRepo = SettingsRepository.getInstance(this)
        fmdServerRepo = FMDServerApiRepository.getInstance(FMDServerApiRepoSpec(this))
        registrationTokensRepo = RegistrationTokenRepository.getInstance(this)

        if (settingsRepo.serverAccountExists()) {
            val fmdServerIntent = Intent(this, FMDServerActivity::class.java)
            finish()
            startActivity(fmdServerIntent)
        }

        val btnOpenWebsite = findViewById<Button>(R.id.buttonOpenFmdServerWebsite)
        btnOpenWebsite.setOnClickListener { _ ->
            openUrl(this, "https://gitlab.com/Nulide/findmydeviceserver")
        }

        // Actively opt-in to using Nulide's server
        val btnUseDefaultServer = findViewById<Button>(R.id.buttonUseDefaultServer)
        btnUseDefaultServer.setOnClickListener { _ -> editTextServerUrl.setText(Settings.DEFAULT_FMD_SERVER_URL) }

        editTextServerUrl = findViewById(R.id.editTextServerUrl)
        editTextServerUrl.addTextChangedListener(this)

        textViewServerVersion = findViewById(R.id.textViewServerVersion)

        btnLogin = findViewById(R.id.buttonLogin)
        btnLogin.setOnClickListener { view: View -> this.onLoginClicked(view) }

        btnRegister = findViewById(R.id.buttonRegister)
        btnRegister.setOnClickListener { view: View -> this.onRegisterClicked(view) }

        val lastKnownServerUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String

        // This must be after btnRegister and btnLogin are assigned,
        // because it causes a call to afterTextChanged, which then accesses btnRegister.
        editTextServerUrl.setText(lastKnownServerUrl)

        getAndShowServerVersion(this, lastKnownServerUrl)
    }

    private fun prefillRegistrationToken(editText: EditText) {
        val serverUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        val cachedToken = registrationTokensRepo.get(serverUrl)
        editText.setText(cachedToken)
    }

    private fun cacheRegistrationToken(token: String) {
        val serverUrl = settingsRepo.get(Settings.SET_FMDSERVER_URL) as String
        registrationTokensRepo.set(serverUrl, token)
    }

    private fun onRegisterClicked(view: View) {
        val context = view.context
        val registerLayout = layoutInflater.inflate(R.layout.dialog_register, null)

        val passwordInput = registerLayout.findViewById<EditText>(R.id.editTextFMDPassword)
        val passwordInputCheck =
            registerLayout.findViewById<EditText>(R.id.editTextFMDPasswordCheck)
        val registrationTokenInput =
            registerLayout.findViewById<EditText>(R.id.editTextRegistrationToken)

        prefillRegistrationToken(registrationTokenInput)

        val registerDialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.Settings_FMDServer_Register))
            .setView(registerLayout)
            .setPositiveButton(getString(R.string.Ok)) { _, _ ->
                showLoadingIndicator(context)

                val password = passwordInput.text.toString()
                val passwordCheck = passwordInputCheck.text.toString()
                val registrationToken = registrationTokenInput.text.toString()

                cacheRegistrationToken(registrationToken)

                if (password.isNotEmpty() && password == passwordCheck) {
                    // Key generation and password hashing is expensive-ish, so we don't want
                    // to do it on the UI thread (e.g., it would block the loading indicator).
                    lifecycleScope.launch(Dispatchers.IO) {
                        val keys = FmdKeyPair.generateNewFmdKeyPair(password)
                        settingsRepo.setKeys(keys)
                        val hashedPW = CypherUtils.hashPasswordForLogin(password)
                        settingsRepo.set(Settings.SET_FMD_CRYPT_HPW, hashedPW)
                        settingsRepo.set(Settings.SET_FMDSERVER_PASSWORD_SET, true)

                        fmdServerRepo.registerAccount(
                            keys.encryptedPrivateKey,
                            keys.base64PublicKey,
                            hashedPW,
                            registrationToken,
                            this@AddAccountActivity::onRegisterOrLoginSuccess,
                            this@AddAccountActivity::onRegisterOrLoginError,
                        )
                    }
                } else {
                    Toast.makeText(context, R.string.pw_change_mismatch, Toast.LENGTH_LONG).show()
                    loadingDialog?.cancel()
                }
            }
        showPrivacyPolicyThenDialog(context, registerDialog)
    }

    private fun onLoginClicked(view: View) {
        val context = view.context
        val loginLayout = layoutInflater.inflate(R.layout.dialog_login, null)

        val idInput = loginLayout.findViewById<EditText>(R.id.editTextFMDID)
        val passwordInput = loginLayout.findViewById<EditText>(R.id.editTextFMDPassword)

        val loginDialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.Settings_FMDServer_Login))
            .setView(loginLayout)
            .setPositiveButton(getString(R.string.Ok)) { _, _ ->
                showLoadingIndicator(context)

                val id = idInput.text.toString()
                val password = passwordInput.text.toString()

                if (id.isNotEmpty() && password.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        fmdServerRepo.login(
                            id,
                            password,
                            this@AddAccountActivity::onRegisterOrLoginSuccess,
                            this@AddAccountActivity::onRegisterOrLoginError,
                        )
                    }
                } else {
                    Toast.makeText(
                        context, R.string.Settings_FMDServer_Error_id_or_pw_empty, Toast.LENGTH_LONG
                    ).show()
                    loadingDialog?.cancel()
                }
            }
        showPrivacyPolicyThenDialog(context, loginDialog)
    }

    private fun showPrivacyPolicyThenDialog(
        context: Context,
        dialogToShowAfterAccepting: AlertDialog.Builder
    ) {
        val webView = WebView(context)
        webView.clearCache(true) // make sure to load the latest policy
        webView.loadUrl(editTextServerUrl.text.toString() + "/ds.html")

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
            .setView(webView)
            .setPositiveButton(getString(R.string.accept)) { _, _ -> dialogToShowAfterAccepting.show() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showLoadingIndicator(context: Context) {
        val loadingLayout = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog =
            MaterialAlertDialogBuilder(context).setView(loadingLayout).setCancelable(false).create()
        loadingDialog?.show()
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
    }

    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
    }

    override fun afterTextChanged(editable: Editable) {
        if (editable === editTextServerUrl.text) {
            var url = editable.toString()
            if (url.endsWith("/")) {
                url = url.substring(0, url.length - 1)
            }
            settingsRepo.set(Settings.SET_FMDSERVER_URL, url)
            if (url.isEmpty()) {
                btnRegister.isEnabled = false
                btnLogin.isEnabled = false
            } else {
                btnRegister.isEnabled = true
                btnLogin.isEnabled = true
            }
            getAndShowServerVersionWithDelay(this, url)
        }
    }

    private fun onRegisterOrLoginSuccess(unit: Unit) {
        runOnUiThread {
            val context = applicationContext
            loadingDialog?.cancel()

            if (!settingsRepo.serverAccountExists()) {
                Toast.makeText(context, "Failed: no user id", Toast.LENGTH_LONG).show()
                return@runOnUiThread
            }

            FMDServerLocationUploadService.scheduleJob(context, 0)
            PushReceiver.registerWithUnifiedPush(context)

            val fmdServerActivityIntent = Intent(context, FMDServerActivity::class.java)
            startActivity(fmdServerActivityIntent)
            finish()
        }
    }

    private fun onRegisterOrLoginError(error: VolleyError) {
        runOnUiThread {
            loadingDialog?.cancel()
            error.printStackTrace()

            var message = ""
            if (error.networkResponse != null) {
                message = """
                ${getString(R.string.request_failed_status_code)}: ${error.networkResponse.statusCode}
                ${getString(R.string.request_failed_response_body)}: ${String(error.networkResponse.data)}
                """.trimIndent()
            }
            message += getString(R.string.request_failed_exception) + ": " + error.message

            if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                message = getString(R.string.server_registration_token_error)
            }

            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(R.string.request_failed_title)
            builder.setMessage(message)
            builder.setNeutralButton(R.string.copy) { _, _ ->
                copyToClipboard(this, getString(R.string.request_failed_title), message)
            }
            builder.setPositiveButton(R.string.Ok) { dialog: DialogInterface, _ -> dialog.dismiss() }
            builder.show()
        }
    }

    private fun getAndShowServerVersionWithDelay(context: Context, serverBaseUrl: String) {
        val DELAY_MILLIS: Long = 1500
        this.lastTextChangedMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

        // Only send the request to the URL if there has been no change within the last DELAY ms.
        // This prevents spamming the server with every keystroke.
        lifecycleScope.launch {
            delay(DELAY_MILLIS)
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
            if (now - lastTextChangedMillis > DELAY_MILLIS) {
                getAndShowServerVersion(context, serverBaseUrl)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getAndShowServerVersion(context: Context, serverBaseUrl: String) {
        if (serverBaseUrl.isEmpty()) {
            textViewServerVersion.text = ""
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            fmdServerRepo.getServerVersion(serverBaseUrl,
                { response: String ->
                    runOnUiThread {
                        var currentString = response
                        if (currentString.startsWith("v")) {
                            currentString = currentString.substring(1)
                        }
                        val minRequired =
                            ComparableVersion(FMDServerApiRepository.MIN_REQUIRED_SERVER_VERSION)
                        val current = ComparableVersion(currentString)

                        if (current < minRequired) {
                            var warningText =
                                context.getString(R.string.server_version_error_low_version)
                            warningText = warningText.replace(
                                "{MIN}",
                                FMDServerApiRepository.MIN_REQUIRED_SERVER_VERSION
                            )
                            warningText = warningText.replace("{CURRENT}", currentString)
                            textViewServerVersion.text = warningText
                        } else {
                            textViewServerVersion.text =
                                "${context.getString(R.string.server_version)}: $currentString"
                        }
                    }
                },
                { error: VolleyError ->
                    runOnUiThread {
                        textViewServerVersion.text =
                            "${context.getString(R.string.server_version_error)}: ${error.message}"
                    }
                }
            )
        }
    }
}
