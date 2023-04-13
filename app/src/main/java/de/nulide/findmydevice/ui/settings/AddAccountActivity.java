package de.nulide.findmydevice.ui.settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.Set;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Keys;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;

public class AddAccountActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher, CompoundButton.OnCheckedChangeListener {

    private RadioButton rbDefaultServer;
    private RadioButton rbCustomServer;
    private EditText etFMDUrl;
    private Button btnLogin;
    private Button btnRegister;

    private Settings settings;

    private Context context;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        this.context = this;

        rbDefaultServer = findViewById(R.id.radioButtonDefaultServer);

        rbCustomServer = findViewById(R.id.radioButtonCustomServer);

        rbDefaultServer.setOnCheckedChangeListener(this);
        rbCustomServer.setOnCheckedChangeListener(this);

        btnLogin = findViewById(R.id.buttonLogin);
        btnLogin.setOnClickListener(this);

        btnRegister = findViewById(R.id.buttonRegister);
        btnRegister.setOnClickListener(this);

        etFMDUrl = findViewById(R.id.editTextFMDServerUrl);
        etFMDUrl.addTextChangedListener(this);
        etFMDUrl.setText((String)settings.get(Settings.SET_FMDSERVER_URL));

        if(!((String)settings.get(Settings.SET_FMDSERVER_URL)).equals(Settings.DEFAULT_SET_FMDSERVER_URL)){
            rbCustomServer.setChecked(true);
        }

    }


    @Override
    public void onClick(View view) {
        WebView webView = new WebView(context);
        webView.loadUrl(etFMDUrl.getText().toString()+"/ds.html");
        LayoutInflater inflater = getLayoutInflater();

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        if (view == btnRegister) {
            alert.setTitle("Register");
            View registerLayout = inflater.inflate(R.layout.register_layout, null);
            alert.setView(registerLayout);
            EditText passwordInput = registerLayout.findViewById(R.id.editTextFMDPassword);
            EditText passwordInputCheck = registerLayout.findViewById(R.id.editTextFMDPasswordCheck);
            alert.setView(registerLayout);
            alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    if (!password.isEmpty() && password.equals(passwordCheck)) {
                        Keys keys = CypherUtils.genKeys(password);
                        settings.setKeys(keys);
                        String hashedPW = CypherUtils.hashWithPKBDF2(password);
                        String splitHash[] = hashedPW.split("///SPLIT///");
                        settings.set(Settings.SET_FMD_CRYPT_HPW, splitHash[1]);
                        settings.setNow(Settings.SET_FMDSERVER_PASSWORD_SET, true);
                        FMDServerService.registerOnServer(context, (String) settings.get(Settings.SET_FMDSERVER_URL), keys.getEncryptedPrivateKey(), keys.getBase64PublicKey(), splitHash[0], splitHash[1]);
                        restartActivityAfterDelay();
                    }else{
                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }else{
            alert.setTitle("Login");
            View loginLayout = inflater.inflate(R.layout.login_layout, null);
            alert.setView(loginLayout);
            EditText idInput = loginLayout.findViewById(R.id.editTextFMDID);
            EditText passwordInput = loginLayout.findViewById(R.id.editTextFMDPassword);
            EditText passwordInputCheck = loginLayout.findViewById(R.id.editTextFMDPasswordCheck);
            alert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String id = idInput.getText().toString();
                    String password = passwordInput.getText().toString();
                    String passwordCheck = passwordInputCheck.getText().toString();
                    if (!id.isEmpty() && !password.isEmpty() && passwordCheck.equals(password)) {
                        FMDServerService.loginOnServer(context, id, password);
                        restartActivityAfterDelay();
                    }else{
                        Toast.makeText(context, "Failed to login.", Toast.LENGTH_LONG).show();
                    }
                }
            });




        }

        AlertDialog.Builder privacyPolicy = new AlertDialog.Builder(context);
        privacyPolicy.setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
                .setView(webView)
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alert.show();

                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable == etFMDUrl.getText()) {
            settings.set(Settings.SET_FMDSERVER_URL, editable.toString());
            if(editable.toString().isEmpty()){
                btnRegister.setEnabled(false);
                btnLogin.setEnabled(false);
            }else{
                btnRegister.setEnabled(true);
                btnLogin.setEnabled(true);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(b) {
            if (compoundButton == rbDefaultServer) {
                etFMDUrl.setEnabled(false);
                etFMDUrl.setText(Settings.DEFAULT_SET_FMDSERVER_URL);
            } else {
                etFMDUrl.setEnabled(true);
            }
        }
    }

    private void restartActivityAfterDelay(){
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
                }
                startActivity(settingIntent);
            }
        }, 1500);
    }
}