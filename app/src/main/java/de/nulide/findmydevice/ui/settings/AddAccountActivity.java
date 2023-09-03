package de.nulide.findmydevice.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
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
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;

public class AddAccountActivity extends AppCompatActivity implements TextWatcher, PostListener {

    private RadioButton rbDefaultServer;
    private RadioButton rbCustomServer;
    private EditText editTextCustomServerUrl;
    private TextView textViewServerVersion;
    private Button btnLogin;
    private Button btnRegister;

    private Settings settings;

    private AlertDialog loadingDialog;

    long lastTextChangedMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        String serverUrl = (String) settings.get(Settings.SET_FMDSERVER_URL);

        rbDefaultServer = findViewById(R.id.radioButtonDefaultServer);
        rbCustomServer = findViewById(R.id.radioButtonCustomServer);

        rbDefaultServer.setOnCheckedChangeListener(this::onCheckedChanged);
        rbCustomServer.setOnCheckedChangeListener(this::onCheckedChanged);

        btnLogin = findViewById(R.id.buttonLogin);
        btnLogin.setOnClickListener(this::onLoginClicked);

        btnRegister = findViewById(R.id.buttonRegister);
        btnRegister.setOnClickListener(this::onRegisterClicked);

        editTextCustomServerUrl = findViewById(R.id.editTextFMDServerUrl);
        editTextCustomServerUrl.addTextChangedListener(this);
        editTextCustomServerUrl.setText(serverUrl);

        textViewServerVersion = findViewById(R.id.textViewServerVersion);

        if (!serverUrl.equals(Settings.DEFAULT_SET_FMDSERVER_URL)) {
            rbCustomServer.setChecked(true);
        }
        getAndShowServerVersion(this, serverUrl);
    }

    private void onRegisterClicked(View view) {
        Context context = view.getContext();
        View registerLayout = getLayoutInflater().inflate(R.layout.dialog_register, null);

        EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
        EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);

        PostListener postListener = this;
        final AlertDialog.Builder registerDialog = new AlertDialog.Builder(context)
                .setTitle("Register")
                .setView(registerLayout)
                .setView(registerLayout)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    showLoadingIndicator(context);

                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    if (!password.isEmpty() && password.equals(passwordCheck)) {
                        new Thread(() -> {
                            FmdKeyPair keys = FmdKeyPair.generateNewFmdKeyPair(password);
                            settings.setKeys(keys);
                            String hashedPW = CypherUtils.hashPasswordForLogin(password);
                            settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
                            settings.setNow(Settings.SET_FMDSERVER_PASSWORD_SET, true);
                            FMDServerService.registerOnServer(context, (String) settings.get(Settings.SET_FMDSERVER_URL), keys.getEncryptedPrivateKey(), keys.getBase64PublicKey(), hashedPW, postListener);
                        }).start();
                    } else {
                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_LONG).show();
                    }
                });
        showPrivacyPolicyThenDialog(context, registerDialog);
    }

    private void onLoginClicked(View view) {
        Context context = view.getContext();
        View loginLayout = getLayoutInflater().inflate(R.layout.dialog_login, null);

        EditText idInput = loginLayout.findViewById(R.id.editTextFMDID);
        EditText passwordInput = loginLayout.findViewById(R.id.editTextFMDPassword);
        EditText passwordInputCheck = loginLayout.findViewById(R.id.editTextFMDPasswordCheck);

        PostListener postListener = this;
        final AlertDialog.Builder loginDialog = new AlertDialog.Builder(context)
                .setTitle("Login")
                .setView(loginLayout)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> {
                    showLoadingIndicator(context);

                    String id = idInput.getText().toString();
                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    if (!id.isEmpty() && !password.isEmpty() && passwordCheck.equals(password)) {
                        new Thread(() -> {
                            FMDServerService.loginOnServer(context, id, password, postListener);
                        }).start();
                    } else {
                        Toast.makeText(context, "Failed to login.", Toast.LENGTH_LONG).show();
                    }
                });
        showPrivacyPolicyThenDialog(context, loginDialog);
    }

    private void showPrivacyPolicyThenDialog(Context context, AlertDialog.Builder dialogToShowAfterAccepting) {
        WebView webView = new WebView(context);
        webView.loadUrl(editTextCustomServerUrl.getText().toString() + "/ds.html");

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
        if (editable == editTextCustomServerUrl.getText()) {
            String url = editable.toString();
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

    private void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (checked) {
            if (compoundButton == rbDefaultServer) {
                editTextCustomServerUrl.setEnabled(false);
                editTextCustomServerUrl.setText(Settings.DEFAULT_SET_FMDSERVER_URL);
                getAndShowServerVersion(this, Settings.DEFAULT_SET_FMDSERVER_URL);
            } else {
                editTextCustomServerUrl.setEnabled(true);
                getAndShowServerVersion(this, editTextCustomServerUrl.getText().toString());
            }
        }
    }

    private void checkForAuth(Context context){
        finish();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                Intent settingIntent = null;
                settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
                if(((String)settings.get(Settings.SET_FMDSERVER_ID)).isEmpty()){
                    settingIntent = new Intent(context, AddAccountActivity.class);
                    Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
                }else{
                    settingIntent = new Intent(context, FMDServerActivity.class);
                    FMDServerService.scheduleJob(context, 0);
                    PushReceiver.registerWithUnifiedPush(context);
                }
                startActivity(settingIntent);
            }
        }, 500);
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
        new Thread(() -> FMDServerService.getServerVersion(context, serverBaseUrl,
                (response) -> runOnUiThread(() -> {
                    String currentString = response;
                    if (currentString.startsWith("v")) {
                        currentString = currentString.substring(1);
                    }
                    ComparableVersion minRequired = new ComparableVersion(FMDServerService.MIN_REQUIRED_SERVER_VERSION);
                    ComparableVersion current = new ComparableVersion(currentString);

                    if (current.compareTo(minRequired) < 0) {
                        String warningText = context.getString(R.string.server_version_error_low_version);
                        warningText = warningText.replace("{MIN}", FMDServerService.MIN_REQUIRED_SERVER_VERSION);
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
