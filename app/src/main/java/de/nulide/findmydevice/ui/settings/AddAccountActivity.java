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

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Calendar;
import java.util.TimeZone;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.FmdKeyPair;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.Utils;

public class AddAccountActivity extends AppCompatActivity implements TextWatcher, PostListener {

    private EditText editTextServerUrl;
    private TextView textViewServerVersion;
    private Button btnLogin;
    private Button btnRegister;

    private Settings settings;

    private AlertDialog loadingDialog;

    private long lastTextChangedMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        String lastKnownServerUrl = (String) settings.get(Settings.SET_FMDSERVER_URL);

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

        PostListener postListener = this;
        final AlertDialog.Builder registerDialog = new AlertDialog.Builder(context)
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
                            settings.setKeys(keys);
                            String hashedPW = CypherUtils.hashPasswordForLogin(password);
                            settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
                            settings.setNow(Settings.SET_FMDSERVER_PASSWORD_SET, true);

                            FMDServerService.registerOnServer(context, keys.getEncryptedPrivateKey(), keys.getBase64PublicKey(), hashedPW, registrationToken, postListener);
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

        PostListener postListener = this;
        final AlertDialog.Builder loginDialog = new AlertDialog.Builder(context)
                .setTitle("Login")
                .setView(loginLayout)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    showLoadingIndicator(context);

                    String id = idInput.getText().toString();
                    String password = passwordInput.getText().toString();

                    if (!id.isEmpty() && !password.isEmpty()) {
                        new Thread(() -> {
                            FMDServerService.loginOnServer(context, id, password, postListener);
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

        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
                .setView(webView)
                .setPositiveButton(getString(R.string.accept), (dialog, which) -> dialogToShowAfterAccepting.show())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showLoadingIndicator(Context context) {
        View loadingLayout = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new AlertDialog.Builder(context).setView(loadingLayout).setCancelable(false).create();
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
            settings.set(Settings.SET_FMDSERVER_URL, url);
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

    @Override
    public void onRestFinished(boolean success) {
        runOnUiThread(() -> {
            Context context = getApplicationContext();
            loadingDialog.cancel();
            if (!success) {
                Toast.makeText(context, "Request failed", Toast.LENGTH_LONG).show();
                return;
            }

            settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
            if (((String) settings.get(Settings.SET_FMDSERVER_ID)).isEmpty()) {
                Toast.makeText(context, "Failed: no user id", Toast.LENGTH_LONG).show();
                return;
            }

            FMDServerService.scheduleJob(context, 0);
            PushReceiver.registerWithUnifiedPush(context);

            Intent fmdServerActivityIntent = new Intent(context, FMDServerActivity.class);
            startActivity(fmdServerActivityIntent);
            finish();
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

        FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(context, serverBaseUrl));
        new Thread(() -> repo.getServerVersion(
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
