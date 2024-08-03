package de.nulide.findmydevice.ui.settings;

import static de.nulide.findmydevice.net.FMDServerApiRepository.MIN_REQUIRED_SERVER_VERSION;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Calendar;
import java.util.TimeZone;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.FmdKeyPair;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.Utils;
import kotlin.Unit;


public class AddAccountActivity extends AppCompatActivity implements TextWatcher {

    private EditText editTextServerUrl;
    private TextView textViewServerVersion;
    private Button btnLogin;
    private Button btnRegister;

    private SettingsRepository settingsRepo;
    private FMDServerApiRepository fmdServerRepo;

    private AlertDialog loadingDialog;

    private long lastTextChangedMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        settingsRepo = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this));

        String fmdServerId = (String) settingsRepo.getSettings().get(Settings.SET_FMDSERVER_ID);
        if (!fmdServerId.isEmpty()) {
            Intent fmdServerIntent = new Intent(this, FMDServerActivity.class);
            finish();
            startActivity(fmdServerIntent);
        }

        String lastKnownServerUrl = (String) settingsRepo.getSettings().get(Settings.SET_FMDSERVER_URL);

        fmdServerRepo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(this));

        Button btnOpenWebsite = findViewById(R.id.buttonOpenFmdServerWebsite);
        btnOpenWebsite.setOnClickListener(v ->
                Utils.openUrl(this, "https://gitlab.com/Nulide/findmydeviceserver"));

        // Actively opt-in to using Nulide's server
        Button btnUseDefaultServer = findViewById(R.id.buttonUseDefaultServer);
        btnUseDefaultServer.setOnClickListener(v ->
                editTextServerUrl.setText(Settings.DEFAULT_FMD_SERVER_URL));

        editTextServerUrl = findViewById(R.id.editTextServerUrl);
        editTextServerUrl.addTextChangedListener(this);

        textViewServerVersion = findViewById(R.id.textViewServerVersion);

        btnLogin = findViewById(R.id.buttonLogin);
        btnLogin.setOnClickListener(this::onLoginClicked);

        btnRegister = findViewById(R.id.buttonRegister);
        btnRegister.setOnClickListener(this::onRegisterClicked);

        // This must be after btnRegister and btnLogin are assigned,
        // because it causes a call to afterTextChanged, which then accesses btnRegister.
        editTextServerUrl.setText(lastKnownServerUrl);

        getAndShowServerVersion(this, lastKnownServerUrl);
    }

    private void onRegisterClicked(View view) {
        Context context = view.getContext();
        View registerLayout = getLayoutInflater().inflate(R.layout.dialog_register, null);

        EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
        EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
        EditText registrationTokenInput = registerLayout.findViewById(R.id.editTextRegistrationToken);

        final AlertDialog.Builder registerDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Register")
                .setView(registerLayout)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    showLoadingIndicator(context);

                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    String registrationToken = registrationTokenInput.getText().toString();

                    if (!password.isEmpty() && password.equals(passwordCheck)) {
                        new Thread(() -> {
                            // Start the thread here. Key generation and password hashing is expensive-ish,
                            // so we don't want to do it on the UI thread (it would block then loading indicator).
                            FmdKeyPair keys = FmdKeyPair.generateNewFmdKeyPair(password);
                            settingsRepo.getSettings().setKeys(keys);
                            String hashedPW = CypherUtils.hashPasswordForLogin(password);
                            settingsRepo.getSettings().set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
                            settingsRepo.getSettings().set(Settings.SET_FMDSERVER_PASSWORD_SET, true);

                            fmdServerRepo.registerAccount(keys.getEncryptedPrivateKey(), keys.getBase64PublicKey(), hashedPW, registrationToken,
                                    this::onRegisterOrLoginSuccess, this::onRegisterOrLoginError
                            );
                        }).start();
                    } else {
                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_LONG).show();
                        loadingDialog.cancel();
                    }
                });
        showPrivacyPolicyThenDialog(context, registerDialog);
    }

    private void onLoginClicked(View view) {
        Context context = view.getContext();
        View loginLayout = getLayoutInflater().inflate(R.layout.dialog_login, null);

        EditText idInput = loginLayout.findViewById(R.id.editTextFMDID);
        EditText passwordInput = loginLayout.findViewById(R.id.editTextFMDPassword);

        final AlertDialog.Builder loginDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Login")
                .setView(loginLayout)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    showLoadingIndicator(context);

                    String id = idInput.getText().toString();
                    String password = passwordInput.getText().toString();

                    if (!id.isEmpty() && !password.isEmpty()) {
                        new Thread(() -> {
                            fmdServerRepo.login(id, password, this::onRegisterOrLoginSuccess, this::onRegisterOrLoginError);
                        }).start();
                    } else {
                        Toast.makeText(context, "FMD ID and password must not be empty.", Toast.LENGTH_LONG).show();
                        loadingDialog.cancel();
                    }
                });
        showPrivacyPolicyThenDialog(context, loginDialog);
    }

    private void showPrivacyPolicyThenDialog(Context context, AlertDialog.Builder dialogToShowAfterAccepting) {
        WebView webView = new WebView(context);
        webView.clearCache(true); // make sure to load the latest policy
        webView.loadUrl(editTextServerUrl.getText().toString() + "/ds.html");

        new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
                .setView(webView)
                .setPositiveButton(getString(R.string.accept), (dialog, which) -> dialogToShowAfterAccepting.show())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showLoadingIndicator(Context context) {
        View loadingLayout = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new MaterialAlertDialogBuilder(context).setView(loadingLayout).setCancelable(false).create();
        loadingDialog.show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable == editTextServerUrl.getText()) {
            String url = editable.toString();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            settingsRepo.getSettings().set(Settings.SET_FMDSERVER_URL, url);
            if (url.isEmpty()) {
                btnRegister.setEnabled(false);
                btnLogin.setEnabled(false);
            } else {
                btnRegister.setEnabled(true);
                btnLogin.setEnabled(true);
            }
            getAndShowServerVersionWithDelay(this, url);
        }
    }

    private void onRegisterOrLoginSuccess(Unit unit) {
        runOnUiThread(() -> {
            Context context = getApplicationContext();
            loadingDialog.cancel();

            if (((String) settingsRepo.getSettings().get(Settings.SET_FMDSERVER_ID)).isEmpty()) {
                Toast.makeText(context, "Failed: no user id", Toast.LENGTH_LONG).show();
                return;
            }

            FMDServerLocationUploadService.scheduleJob(context, 0);
            PushReceiver.registerWithUnifiedPush(context);

            Intent fmdServerActivityIntent = new Intent(context, FMDServerActivity.class);
            startActivity(fmdServerActivityIntent);
            finish();
        });
    }

    private void onRegisterOrLoginError(VolleyError error) {
        runOnUiThread(() -> {
            loadingDialog.cancel();
            error.printStackTrace();

            String message = "";
            if (error.networkResponse != null) {
                message = getString(R.string.request_failed_status_code) + ": " + error.networkResponse.statusCode + "\n"
                        + getString(R.string.request_failed_response_body) + ": " + new String(error.networkResponse.data) + "\n";
            }
            message += getString(R.string.request_failed_exception) + ": " + error.getMessage();
            String finalMessage = message; // needed to be able to use it in Lambda

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.request_failed_title);
            builder.setMessage(finalMessage);
            builder.setNeutralButton(R.string.copy, (dialog, which) -> {
                Utils.copyToClipboard(this, getString(R.string.request_failed_title), finalMessage);
            });
            builder.setPositiveButton(R.string.Ok, (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }

    private void getAndShowServerVersionWithDelay(Context context, String serverBaseUrl) {
        long DELAY_MILLIS = 1500;
        this.lastTextChangedMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
        // Only send the request to the URL if there has been no change within the last DELAY ms.
        // This prevents spamming the server with every keystroke.
        new Handler().postDelayed(() -> {
            long now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
            if (now - this.lastTextChangedMillis > DELAY_MILLIS) {
                getAndShowServerVersion(context, serverBaseUrl);
            }
            // If there has been a recent change, just exit. That newer change will also have launched a postDelayed.
        }, DELAY_MILLIS);
    }

    private void getAndShowServerVersion(Context context, String serverBaseUrl) {
        if (serverBaseUrl.isEmpty()) {
            textViewServerVersion.setText("");
            return;
        }

        new Thread(() -> fmdServerRepo.getServerVersion(serverBaseUrl,
                (response) -> runOnUiThread(() -> {
                    String currentString = response;
                    if (currentString.startsWith("v")) {
                        currentString = currentString.substring(1);
                    }
                    ComparableVersion minRequired = new ComparableVersion(MIN_REQUIRED_SERVER_VERSION);
                    ComparableVersion current = new ComparableVersion(currentString);

                    if (current.compareTo(minRequired) < 0) {
                        String warningText = context.getString(R.string.server_version_error_low_version);
                        warningText = warningText.replace("{MIN}", MIN_REQUIRED_SERVER_VERSION);
                        warningText = warningText.replace("{CURRENT}", currentString);
                        textViewServerVersion.setText(warningText);
                    } else {
                        String prefix = context.getString(R.string.server_version);
                        String text = prefix + ": " + currentString;
                        textViewServerVersion.setText(text);
                    }
                }),
                (error) -> runOnUiThread(() -> {
                    String prefix = context.getString(R.string.server_version_error);
                    String text = prefix + ": " + error.getMessage();
                    textViewServerVersion.setText(text);
                })
        )).start();
    }
}
