package de.nulide.findmydevice.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikepenz.aboutlibraries.LibsBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.ui.IntroductionActivity;
import de.nulide.findmydevice.ui.LogActivity;
import de.nulide.findmydevice.ui.helper.SettingsEntry;
import de.nulide.findmydevice.ui.helper.SettingsViewAdapter;

public class SettingsActivity extends AppCompatActivity {

    private final int EXPORT_REQ_CODE = 30;
    private final int IMPORT_REQ_CODE = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        List<SettingsEntry> settingsEntries = SettingsEntry.getSettingsEntries(this);

        ListView listSettings = findViewById(R.id.listSettings);
        listSettings.setAdapter(new SettingsViewAdapter(this, settingsEntries));
        listSettings.setOnItemClickListener(this::onItemClick);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();

        Intent settingIntent = null;
        switch (position) {
            case 0:
                settingIntent = new Intent(this, FMDConfigActivity.class);
                break;
            case 1:
                if (settings.isEmpty(Settings.SET_FMDSERVER_ID)) {
                    settingIntent = new Intent(this, AddAccountActivity.class);
                } else {
                    settingIntent = new Intent(this, FMDServerActivity.class);
                }
                break;
            case 2:
                settingIntent = new Intent(this, WhiteListActivity.class);
                break;
            case 3:
                settingIntent = new Intent(this, OpenCellIdActivity.class);
                break;
            case 4:
                settingIntent = new Intent(this, IntroductionActivity.class);
                settingIntent.putExtra(IntroductionActivity.POS_KEY, 1);
                break;
            case 5:
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.putExtra(Intent.EXTRA_TITLE, IO.settingsFileName);
                intent.setType("*/*");
                startActivityForResult(intent, EXPORT_REQ_CODE);
                break;
            case 6:
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(intent, IMPORT_REQ_CODE);
                break;
            case 7:
                settingIntent = new Intent(this, LogActivity.class);
                break;
            case 8:
                String activityTitle = getString(R.string.Settings_About);
                settingIntent = new LibsBuilder().withActivityTitle(activityTitle).withListener(AboutLibsListener.listener).intent(this);
                break;
        }
        if (settingIntent != null) {
            startActivity(settingIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);


                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder json = new StringBuilder();
                    try {
                        String line;

                        while ((line = br.readLine()) != null) {
                            json.append(line);
                            json.append('\n');
                        }
                        br.close();
                        String text = json.toString();
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        if (!text.isEmpty()) {
                            Settings settings = mapper.readValue(text, Settings.class);
                            settings.set(Settings.SET_INTRODUCTION_VERSION, settings.get(Settings.SET_INTRODUCTION_VERSION));
                            finish();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == EXPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Settings.writeToUri(this, uri);
            }
        }
    }
}
