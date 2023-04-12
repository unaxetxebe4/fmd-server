package de.nulide.findmydevice.ui.settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;

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
        if (view == btnRegister) {
            WebView webView = new WebView(context);
            webView.loadUrl(etFMDUrl.getText().toString()+"/ds.html");

            final AlertDialog.Builder pinAlert = new AlertDialog.Builder(this);
            pinAlert.setTitle(getString(R.string.FMDConfig_Alert_Password));
            pinAlert.setMessage(getString(R.string.Settings_Enter_Password));
            final EditText input = new EditText(this);
            input.setTransformationMethod(new PasswordTransformationMethod());
            pinAlert.setView(input);
            pinAlert.setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String text = input.getText().toString();
                    if (!text.isEmpty()) {
                        Keys keys = CypherUtils.genKeys(text);
                        settings.setKeys(keys);
                        String hashedPW = CypherUtils.hashWithPKBDF2(text);
                        String splitHash[] = hashedPW.split("///SPLIT///");
                        settings.set(Settings.SET_FMD_CRYPT_HPW, splitHash[1]);
                        settings.setNow(Settings.SET_FMDSERVER_PASSWORD_SET, true);
                        FMDServerService.registerOnServer(context, (String) settings.get(Settings.SET_FMDSERVER_URL), keys.getEncryptedPrivateKey(), keys.getBase64PublicKey(), splitHash[0], splitHash[1]);
                        finish();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1500);
                    }
                }
            });

            AlertDialog.Builder privacyPolicy = new AlertDialog.Builder(context);
            privacyPolicy.setTitle(getString(R.string.Settings_FMDServer_Alert_PrivacyPolicy_Title))
                    .setView(webView)
                    .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pinAlert.show();

                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

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
}