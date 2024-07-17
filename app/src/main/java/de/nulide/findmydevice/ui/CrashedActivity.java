package de.nulide.findmydevice.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.LogData;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONLog;
import de.nulide.findmydevice.utils.Utils;

public class CrashedActivity extends AppCompatActivity {

    private String crashLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crashed);

        IO.context = this;
        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();

        LogData log = JSONFactory.convertJSONLog(IO.read(JSONLog.class, IO.logFileName));
        Integer index = (Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY);
        // Double check that the index is in range.
        // We saw a few "java.lang.IndexOutOfBoundsException: Index: -1".
        if (index < 0) {
            continueToMain();
            return;
        }

        crashLog = log.get(index).getText();
        settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, -1);

        TextView textViewCrashLog = findViewById(R.id.textViewCrash);
        textViewCrashLog.setText(crashLog);

        Button buttonSendLog = findViewById(R.id.buttonSendLog);
        buttonSendLog.setOnClickListener(this::onSendLogClicked);

        Button buttonCopy = findViewById(R.id.buttonCopyLog);
        buttonCopy.setOnClickListener(this::onCopyClicked);

        Button buttonContinue = findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(this::onContinueClicked);
    }

    private void onSendLogClicked(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"Null@nulide.de"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "CrashLog");
        intent.putExtra(Intent.EXTRA_TEXT, crashLog);
        startActivity(intent);
        finish();
    }

    private void onCopyClicked(View v) {
        Utils.copyToClipboard(v.getContext(), "CrashLog", crashLog);
    }

    private void onContinueClicked(View v) {
        continueToMain();
    }

    private void continueToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
