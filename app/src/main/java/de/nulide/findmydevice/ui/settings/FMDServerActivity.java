package de.nulide.findmydevice.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
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
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;

public class FMDServerActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher, View.OnClickListener {

    private Settings settings;

    private CheckBox checkBoxFMDServerAutoUpload;
    private EditText editTextFMDServerUpdateTime;
    private TextView textViewFMDServerID;
    private Button changePasswordButton;
    private Button logoutButton;
    private Button deleteButton;
    private CheckBox checkBoxFMDServerGPS;
    private CheckBox checkBoxFMDServerCell;
    private CheckBox checkBoxLowBat;

    private int colorEnabled;
    private int colorDisabled;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_server);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        this.context = this;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            colorEnabled = getColor(R.color.colorEnabled);
            colorDisabled = getColor(R.color.colorDisabled);
        } else {
            colorEnabled = getResources().getColor(R.color.colorEnabled);
            colorDisabled = getResources().getColor(R.color.colorDisabled);
        }

        checkBoxFMDServerAutoUpload = findViewById(R.id.checkBoxFMDServerAutoUpload);
        checkBoxFMDServerAutoUpload.setChecked((Boolean) settings.get(Settings.SET_FMDSERVER_AUTO_UPLOAD));
        checkBoxFMDServerAutoUpload.setOnCheckedChangeListener(this);

        editTextFMDServerUpdateTime = findViewById(R.id.editTextFMDServerUpdateTime);
        editTextFMDServerUpdateTime.setText(((Integer) settings.get(Settings.SET_FMDSERVER_UPDATE_TIME)).toString());
        editTextFMDServerUpdateTime.addTextChangedListener(this);

        textViewFMDServerID = findViewById(R.id.textViewID);

        changePasswordButton = findViewById(R.id.buttonChangePassword);
        changePasswordButton.setOnClickListener(this);

        logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(this);

        deleteButton = findViewById(R.id.buttonDeleteData);
        deleteButton.setOnClickListener(this);

        if (!((String) settings.get(Settings.SET_FMDSERVER_ID)).isEmpty()) {
            textViewFMDServerID.setText((String) settings.get(Settings.SET_FMDSERVER_ID));
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setEnabled(true);
        }

        if(!(Boolean) settings.get(Settings.SET_FIRST_TIME_FMD_SERVER)) {
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
        switch((Integer)settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE)){
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
        if((Boolean)settings.get(Settings.SET_FMD_LOW_BAT_SEND)){
            checkBoxLowBat.setChecked(true);
        }else{
            checkBoxLowBat.setChecked(false);
        }
        checkBoxLowBat.setOnCheckedChangeListener(this);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView == checkBoxFMDServerAutoUpload){
            settings.set(Settings.SET_FMDSERVER_AUTO_UPLOAD, isChecked);
        }else if(buttonView == checkBoxFMDServerCell || buttonView == checkBoxFMDServerGPS){
            if(checkBoxFMDServerGPS.isChecked() && checkBoxFMDServerCell.isChecked()){
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 2);
            }else if(checkBoxFMDServerGPS.isChecked()){
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 0);
            }else if(checkBoxFMDServerCell.isChecked()){
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 1);
            }else{
                settings.set(Settings.SET_FMDSERVER_LOCATION_TYPE, 0);
            }
        }else if(buttonView == checkBoxLowBat){
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

    @Override
    public void onClick(View v) {
        if(v == deleteButton){
            AlertDialog.Builder privacyPolicy = new AlertDialog.Builder(context);
            privacyPolicy.setTitle(getString(R.string.Settings_FMDServer_Alert_DeleteData))
                    .setMessage(R.string.Settings_FMDServer_Alert_DeleteData_Desc)
                    .setPositiveButton(getString(R.string.Ok), new DialogClickListenerForUnregistration(this))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }else if (v == logoutButton) {
            settings.set(Settings.SET_FMDSERVER_ID, "");
            settings.set(Settings.SET_FMD_CRYPT_HPW, "");
            settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, "");
            settings.set(Settings.SET_FMD_CRYPT_PUBKEY, "");
            finish();
        } else if (v == changePasswordButton) {
            LayoutInflater inflater = getLayoutInflater();
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Change Password");
            View registerLayout = inflater.inflate(R.layout.password_change_layout, null);
            alert.setView(registerLayout);
            EditText oldPasswordInput = registerLayout.findViewById(R.id.editTextFMDOldPassword);
            EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
            EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
            alert.setView(registerLayout);
            alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                public void onClick(DialogInterface dialog, int whichButton) {
                    String oldPassword = oldPasswordInput.getText().toString();
                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    if (!password.isEmpty() && password.equals(passwordCheck) && !oldPassword.isEmpty()) {
                        PrivateKey privKey = CypherUtils.decryptKey((String) settings.get(Settings.SET_FMD_CRYPT_PRIVKEY), oldPassword);
                        if(privKey == null){
                            Toast.makeText(context, "Wrong Password.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String newPrivKey = CypherUtils.encryptKey(privKey, password);
                        String hashedPW = CypherUtils.hashWithPKBDF2(password);
                        String[] splitHash = hashedPW.split("///SPLIT///");

                        FMDServerService.changePassword(context, newPrivKey, splitHash[0], splitHash[1]);


                    }else{
                        Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
                    }
                }
            });
            alert.show();
        }
    }

    private class DialogClickListenerForUnregistration implements DialogInterface.OnClickListener{

        private Context context;

        public DialogClickListenerForUnregistration(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            FMDServerService.unregisterOnServer(context);
            finish();
            startActivity(getIntent());
        }
    }

}