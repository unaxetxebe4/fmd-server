package de.nulide.findmydevice.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.security.PrivateKey;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.UnregisterUtil;
import de.nulide.findmydevice.utils.Utils;

public class FMDServerActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher {

    private Settings settings;
    private FMDServerApiRepository fmdServerRepo;

    private TextView textViewServerUrl;
    private TextView textViewUserId;
    private ImageView buttonCopyServerUrl;
    private ImageView buttonCopyUserId;
    private TextView textViewConnectionStatus;
    private Button changePasswordButton;
    private Button logoutButton;
    private Button deleteButton;

    private TextView textViewPushHelp;
    private Button openUnifiedPushButton;

    private EditText editTextFMDServerUpdateTime;

    private CheckBox checkBoxFMDServerGPS;
    private CheckBox checkBoxFMDServerCell;

    private CheckBox checkBoxLowBat;

    private Context context;

    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_server);

        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        fmdServerRepo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(this));
        this.context = this;

        textViewServerUrl = findViewById(R.id.textViewServerUrl);
        textViewUserId = findViewById(R.id.textViewUserId);
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);
        textViewServerUrl.setText((String) settings.get(Settings.SET_FMDSERVER_URL));
        textViewUserId.setText((String) settings.get(Settings.SET_FMDSERVER_ID));

        findViewById(R.id.buttonOpenWebClient).setOnClickListener(this::onOpenWebClientClicked);
        findViewById(R.id.buttonCopyServerUrl).setOnClickListener(this::onCopyServerUrlClicked);
        findViewById(R.id.buttonCopyUserId).setOnClickListener(this::onCopyUserIdClicked);

        changePasswordButton = findViewById(R.id.buttonChangePassword);
        changePasswordButton.setOnClickListener(this::onChangePasswordClicked);

        logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(this::onLogoutClicked);

        deleteButton = findViewById(R.id.buttonDeleteData);
        deleteButton.setOnClickListener(this::onDeleteClicked);

        textViewPushHelp = findViewById(R.id.textPushHelp);

        openUnifiedPushButton = findViewById(R.id.buttonOpenUnifiedPush);
        openUnifiedPushButton.setOnClickListener(this::onOpenUnifiedPushClicked);

        editTextFMDServerUpdateTime = findViewById(R.id.editTextFMDServerUpdateTime);
        editTextFMDServerUpdateTime.setText(((Integer) settings.get(Settings.SET_FMDSERVER_UPDATE_TIME)).toString());
        editTextFMDServerUpdateTime.addTextChangedListener(this);

        checkBoxFMDServerGPS = findViewById(R.id.checkBoxFMDServerGPS);
        checkBoxFMDServerCell = findViewById(R.id.checkBoxFMDServerCell);
        switch ((Integer) settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE)) {
            case 0:
                checkBoxFMDServerGPS.setChecked(true);
                checkBoxFMDServerCell.setChecked(false);
                break;
            case 1:
                checkBoxFMDServerGPS.setChecked(false);
                checkBoxFMDServerCell.setChecked(true);
                break;
            case 2:
                checkBoxFMDServerGPS.setChecked(true);
                checkBoxFMDServerCell.setChecked(true);
                break;
            case 3:
                checkBoxFMDServerGPS.setChecked(false);
                checkBoxFMDServerCell.setChecked(false);
                break;
        }
        checkBoxFMDServerGPS.setOnCheckedChangeListener(this);
        checkBoxFMDServerCell.setOnCheckedChangeListener(this);

        checkBoxLowBat = findViewById(R.id.checkBoxFMDServerLowBatUpload);
        checkBoxLowBat.setChecked((Boolean) settings.get(Settings.SET_FMD_LOW_BAT_SEND));
        checkBoxLowBat.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PushReceiver.registerWithUnifiedPush(this);

        if (PushReceiver.isRegisteredWithUnifiedPush(this)) {
            textViewPushHelp.setText(R.string.Settings_FMDServer_Push_Description_Available);
        } else {
            textViewPushHelp.setText(R.string.Settings_FMDServer_Push_Description_Missing);
        }

        checkConnection();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBoxFMDServerCell || buttonView == checkBoxFMDServerGPS) {
            if (checkBoxFMDServerGPS.isChecked() && checkBoxFMDServerCell.isChecked()) {
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 2);
            } else if (checkBoxFMDServerGPS.isChecked()) {
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 0);
            } else if (checkBoxFMDServerCell.isChecked()) {
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 1);
            } else {
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 3);
            }
        } else if (buttonView == checkBoxLowBat) {
            settings.set(Settings.SET_FMD_LOW_BAT_SEND, isChecked);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable edited) {
        if (edited == editTextFMDServerUpdateTime.getText()) {
            if (edited.toString().isEmpty()) {
                settings.set(Settings.SET_FMDSERVER_UPDATE_TIME, 60);
            } else {
                settings.set(Settings.SET_FMDSERVER_UPDATE_TIME, Integer.parseInt(editTextFMDServerUpdateTime.getText().toString()));
            }
        }
    }

    private void onOpenWebClientClicked(View view) {
        String url = (String) settings.get(Settings.SET_FMDSERVER_URL);
        Utils.openUrl(this, url);
    }

    private void onCopyServerUrlClicked(View view) {
        String label = getString(R.string.Settings_FMD_Server_Server_URL).replace(":", "");
        String text = (String) settings.get(Settings.SET_FMDSERVER_URL);
        Utils.copyToClipboard(this, label, text);
    }

    private void onCopyUserIdClicked(View view) {
        String label = getString(R.string.Settings_FMD_Server_User_ID).replace(":", "");
        String text = (String) settings.get(Settings.SET_FMDSERVER_ID);
        Utils.copyToClipboard(this, label, text);
    }

    private void onDeleteClicked(View view) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.Settings_FMDServer_Alert_DeleteData))
                .setMessage(R.string.Settings_FMDServer_Alert_DeleteData_Desc)
                .setPositiveButton(getString(R.string.Ok), (dialog, whichButton) -> runDelete())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void onLogoutClicked(View view) {
        settings.set(Settings.SET_FMDSERVER_ID, "");
        settings.set(Settings.SET_FMD_CRYPT_HPW, "");
        settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, "");
        settings.set(Settings.SET_FMD_CRYPT_PUBKEY, "");
        FMDServerLocationUploadService.cancelJob(this);
        finish();
    }

    private void onChangePasswordClicked(View view) {
        LayoutInflater inflater = getLayoutInflater();
        final AlertDialog.Builder alert = new MaterialAlertDialogBuilder(this);
        alert.setTitle("Change Password");
        View registerLayout = inflater.inflate(R.layout.dialog_password_change, null);
        alert.setView(registerLayout);
        EditText oldPasswordInput = registerLayout.findViewById(R.id.editTextFMDOldPassword);
        EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
        EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
        alert.setView(registerLayout);

        alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String oldPassword = oldPasswordInput.getText().toString();
                String password = passwordInput.getText().toString();
                String passwordCheck = passwordInputCheck.getText().toString();

                if (!password.isEmpty() && password.equals(passwordCheck) && !oldPassword.isEmpty()) {
                    runChangePassword(oldPassword, password);
                } else {
                    Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.show();
    }

    private void showLoadingIndicator(Context context) {
        View loadingLayout = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new MaterialAlertDialogBuilder(context).setView(loadingLayout).setCancelable(false).create();
        loadingDialog.show();
    }

    private void onOpenUnifiedPushClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://unifiedpush.org/"));
        startActivity(intent);
    }

    private void runChangePassword(String oldPassword, String password) {
        showLoadingIndicator(context);
        // do expensive async crypto and hashing in a background thread (not on the UI thread)
        new Thread(() -> {
            try {
                PrivateKey privKey = CypherUtils.decryptPrivateKeyWithPassword((String) settings.get(Settings.SET_FMD_CRYPT_PRIVKEY), oldPassword);
                if (privKey == null) {
                    Toast.makeText(context, "Wrong Password.", Toast.LENGTH_LONG).show();
                    loadingDialog.cancel();
                    return;
                }
                String newPrivKey = CypherUtils.encryptPrivateKeyWithPassword(privKey, password);
                String hashedPW = CypherUtils.hashPasswordForLogin(password);

                runOnUiThread(() -> {
                    fmdServerRepo.changePassword(hashedPW, newPrivKey,
                            (response -> {
                                loadingDialog.cancel();
                                Toast.makeText(context, "Success", Toast.LENGTH_LONG).show();
                            }),
                            (error) -> {
                                Toast.makeText(context, "Request failed", Toast.LENGTH_LONG).show();
                                loadingDialog.cancel();
                            });
                });
            } catch (Exception bdp) {
                runOnUiThread(() -> {
                    Toast.makeText(context, "Wrong Password.", Toast.LENGTH_LONG).show();
                    loadingDialog.cancel();
                });
            }
        }).start();
    }

    private void runDelete() {
        showLoadingIndicator(context);
        FMDServerLocationUploadService.cancelJob(context);
        fmdServerRepo.unregister(
                response -> {
                    loadingDialog.cancel();
                    Toast.makeText(context, "Unregister successful", Toast.LENGTH_LONG).show();
                    finish();
                }, error -> {
                    loadingDialog.cancel();
                    UnregisterUtil.showUnregisterFailedDialog(context, error, this::finish);
                }
        );
    }

    private void checkConnection() {
        // Check if we can connect to the server and can log in (i.e., get an access token)
        fmdServerRepo.getAccessToken(
                response -> {
                    textViewConnectionStatus.setText(R.string.Settings_FMD_Server_Connection_Status_Success);
                    textViewConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.md_theme_primary));
                    textViewConnectionStatus.setOnClickListener(v -> {
                    });
                },
                error -> {
                    textViewConnectionStatus.setText(error.toString());
                    textViewConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.md_theme_error));
                    textViewConnectionStatus.setOnClickListener(v -> {
                        Utils.copyToClipboard(this, "", error.toString());
                    });
                }
        );
    }
}