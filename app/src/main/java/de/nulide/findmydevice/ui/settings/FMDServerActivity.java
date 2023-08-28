package de.nulide.findmydevice.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.security.PrivateKey;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.UnregisterUtil;
import de.nulide.findmydevice.utils.Utils;

public class FMDServerActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher, PostListener {

    private Settings settings;

    private TextView textViewServerUrl;
    private TextView textViewUserId;
    private ImageView buttonCopyServerUrl;
    private ImageView buttonCopyUserId;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_server);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        this.context = this;

        textViewServerUrl = findViewById(R.id.textViewServerUrl);
        textViewUserId = findViewById(R.id.textViewUserId);
        textViewServerUrl.setText((String) settings.get(Settings.SET_FMDSERVER_URL));
        textViewUserId.setText((String) settings.get(Settings.SET_FMDSERVER_ID));

        buttonCopyServerUrl = findViewById(R.id.buttonCopyServerUrl);
        buttonCopyUserId = findViewById(R.id.buttonCopyUserId);
        buttonCopyServerUrl.setOnClickListener(this::onCopyServerUrlClicked);
        buttonCopyUserId.setOnClickListener(this::onCopyUserIdClicked);

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


        if (!(Boolean) settings.get(Settings.SET_FIRST_TIME_FMD_SERVER)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.Settings_FMDServer))
                    .setMessage(this.getString(R.string.Alert_First_time_fmdserver))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            settings.set(Settings.SET_FIRST_TIME_FMD_SERVER, true);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }

        checkBoxFMDServerGPS = findViewById(R.id.checkBoxFMDServerGPS);
        checkBoxFMDServerCell = findViewById(R.id.checkBoxFMDServerCell);
        switch ((Integer) settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE)) {
            case 0:
                checkBoxFMDServerGPS.setChecked(true);
                break;
            case 1:
                checkBoxFMDServerCell.setChecked(true);
                break;
            case 2:
                checkBoxFMDServerGPS.setChecked(true);
                checkBoxFMDServerCell.setChecked(true);
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
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 0);
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
        AlertDialog.Builder privacyPolicy = new AlertDialog.Builder(context);
        privacyPolicy.setTitle(getString(R.string.Settings_FMDServer_Alert_DeleteData))
                .setMessage(R.string.Settings_FMDServer_Alert_DeleteData_Desc)
                .setPositiveButton(getString(R.string.Ok), new DialogClickListenerForUnregistration(this))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void onLogoutClicked(View view) {
        settings.set(Settings.SET_FMDSERVER_ID, "");
        settings.set(Settings.SET_FMD_CRYPT_HPW, "");
        settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, "");
        settings.set(Settings.SET_FMD_CRYPT_PUBKEY, "");
        FMDServerService.cancelAll(this);
        finish();
    }

    private void onChangePasswordClicked(View view) {
        LayoutInflater inflater = getLayoutInflater();
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Change Password");
        View registerLayout = inflater.inflate(R.layout.dialog_password_change, null);
        alert.setView(registerLayout);
        EditText oldPasswordInput = registerLayout.findViewById(R.id.editTextFMDOldPassword);
        EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
        EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
        alert.setView(registerLayout);
        PostListener postListener = this;

        alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onClick(DialogInterface dialog, int whichButton) {
                String oldPassword = oldPasswordInput.getText().toString();
                String password = passwordInput.getText().toString();
                String passwordCheck = passwordInputCheck.getText().toString();
                if (!password.isEmpty() && password.equals(passwordCheck) && !oldPassword.isEmpty()) {
                    try {
                        PrivateKey privKey = CypherUtils.decryptPrivateKeyWithPassword((String) settings.get(Settings.SET_FMD_CRYPT_PRIVKEY), oldPassword);
                        if (privKey == null) {
                            Toast.makeText(context, "Wrong Password.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String newPrivKey = CypherUtils.encryptPrivateKeyWithPassword(privKey, password);
                        String hashedPW = CypherUtils.hashPasswordForLogin(password);

                        FMDServerService.changePassword(context, newPrivKey, hashedPW, postListener);
                    } catch (Exception bdp) {
                        Toast.makeText(context, "Wrong Password.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.show();
    }

    private void onOpenUnifiedPushClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://unifiedpush.org/"));
        startActivity(intent);
    }

    @Override
    public void onRestFinished(boolean success) {
        if (success) {
            Toast.makeText(context, "Success", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
        }
        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
    }

    private class DialogClickListenerForUnregistration implements DialogInterface.OnClickListener {

        private final Context context;

        public DialogClickListenerForUnregistration(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            FMDServerService.unregisterOnServer(context, response -> {
                FMDServerService.cancelAll(context);
                finish();
            }, error -> {
                UnregisterUtil.showUnregisterFailedDialog(context, error, () -> {
                    FMDServerService.cancelAll(context);
                    finish();
                });
            });
        }
    }

}