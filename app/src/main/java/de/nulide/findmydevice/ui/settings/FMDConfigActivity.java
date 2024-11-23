package de.nulide.findmydevice.ui.settings;

import static de.nulide.findmydevice.ui.UiUtil.setupEdgeToEdgeAppBar;
import static de.nulide.findmydevice.ui.UiUtil.setupEdgeToEdgeScrollView;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.ui.FmdActivity;
import de.nulide.findmydevice.utils.CypherUtils;

public class FMDConfigActivity extends FmdActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher {

    private SettingsRepository settings;

    private CheckBox checkBoxDeviceWipe;
    private CheckBox checkBoxAccessViaPin;
    private Button buttonEnterPin;
    private Button buttonSelectRingtone;
    private EditText editTextLockScreenMessage;
    private EditText editTextFmdCommand;

    int colorEnabled;
    int colorDisabled;
    int textColorEnabled;
    int textColorDisabled;

    private final int REQUEST_CODE_RINGTONE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_f_m_d_config);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        settings = SettingsRepository.Companion.getInstance(this);

        checkBoxDeviceWipe = findViewById(R.id.checkBoxWipeData);
        checkBoxDeviceWipe.setChecked((Boolean) settings.get(Settings.SET_WIPE_ENABLED));
        checkBoxDeviceWipe.setOnCheckedChangeListener(this);

        checkBoxAccessViaPin = findViewById(R.id.checkBoxFMDviaPin);
        checkBoxAccessViaPin.setChecked((Boolean) settings.get(Settings.SET_ACCESS_VIA_PIN));
        checkBoxAccessViaPin.setOnCheckedChangeListener(this);

        editTextLockScreenMessage = findViewById(R.id.editTextTextLockScreenMessage);
        editTextLockScreenMessage.setText((String) settings.get(Settings.SET_LOCKSCREEN_MESSAGE));
        editTextLockScreenMessage.addTextChangedListener(this);

        colorEnabled = getColor(R.color.colorPrimary);
        colorDisabled = getColor(R.color.md_theme_error);
        textColorEnabled = getColor(R.color.md_theme_onPrimary);
        textColorDisabled = getColor(R.color.md_theme_onError);

        buttonEnterPin = findViewById(R.id.buttonEnterPin);
        if (settings.get(Settings.SET_PIN).equals("")) {
            buttonEnterPin.setBackgroundColor(colorDisabled);
            buttonEnterPin.setTextColor(textColorDisabled);
        } else {
            buttonEnterPin.setBackgroundColor(colorEnabled);
            buttonEnterPin.setTextColor(textColorEnabled);
        }
        buttonEnterPin.setOnClickListener(this::onEnterPinClicked);

        buttonSelectRingtone = findViewById(R.id.buttonSelectRingTone);
        buttonSelectRingtone.setOnClickListener(this::onSelectRingtoneClicked);

        editTextFmdCommand = findViewById(R.id.editTextFmdCommand);
        editTextFmdCommand.setText((String) settings.get(Settings.SET_FMD_COMMAND));
        editTextFmdCommand.addTextChangedListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBoxDeviceWipe) {
            settings.set(Settings.SET_WIPE_ENABLED, isChecked);
        } else if (buttonView == checkBoxAccessViaPin) {
            settings.set(Settings.SET_ACCESS_VIA_PIN, isChecked);
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
        if (edited == editTextLockScreenMessage.getText()) {
            settings.set(Settings.SET_LOCKSCREEN_MESSAGE, edited.toString());
        } else if (edited == editTextFmdCommand.getText()) {
            if (edited.toString().isEmpty()) {
                Toast.makeText(this, getString(R.string.Toast_Empty_FMDCommand), Toast.LENGTH_LONG).show();
                settings.set(Settings.SET_FMD_COMMAND, "fmd");
            } else {
                settings.set(Settings.SET_FMD_COMMAND, edited.toString().toLowerCase());
            }
        }
    }

    private void onEnterPinClicked(View v) {
        Context context = v.getContext();
        View pinLayout = getLayoutInflater().inflate(R.layout.dialog_pin, null);

        EditText editTextPin = pinLayout.findViewById(R.id.editTextPin);
        EditText editTextPinRepeat = pinLayout.findViewById(R.id.editTextPinRepeat);

        new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.Settings_Enter_Pin))
                .setView(pinLayout)
                .setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String pin = editTextPin.getText().toString();
                        String repeat = editTextPinRepeat.getText().toString();

                        if (pin.equals(repeat)) {
                            if (pin.isEmpty()) {
                                settings.set(Settings.SET_PIN, "");
                                buttonEnterPin.setBackgroundColor(colorDisabled);
                                buttonEnterPin.setTextColor(textColorDisabled);
                            } else {
                                settings.set(Settings.SET_PIN, CypherUtils.hashPasswordForFmdPin(pin));
                                buttonEnterPin.setBackgroundColor(colorEnabled);
                                buttonEnterPin.setTextColor(textColorEnabled);
                            }
                        } else {
                            Toast.makeText(context, "PINs do not match", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void onSelectRingtoneClicked(View v) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.Settings_Select_Ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse((String) settings.get(Settings.SET_RINGER_TONE)));
        try {
            this.startActivityForResult(intent, REQUEST_CODE_RINGTONE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.Settings_no_ringtone_picker), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_RINGTONE) {
            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            settings.set(Settings.SET_RINGER_TONE, uri.toString());
        }
    }

}