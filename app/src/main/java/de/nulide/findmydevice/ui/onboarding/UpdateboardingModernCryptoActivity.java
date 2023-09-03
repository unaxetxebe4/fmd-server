package de.nulide.findmydevice.ui.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.ui.MainActivity;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.UnregisterUtil;

public class UpdateboardingModernCryptoActivity extends AppCompatActivity {

    private final int EXPORT_REQ_CODE = 30;

    boolean isRegisteredWithServer;
    boolean isPinSet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updateboarding_modern_crypto);

        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        isRegisteredWithServer = settings.checkAccountExists();
        isPinSet = !settings.get(Settings.SET_PIN).equals("");

        if (!isRegisteredWithServer && !isPinSet) {
            completeAndContinueToMain();
        } else {
            if (!isPinSet) {
                findViewById(R.id.sectionFMDPin).setVisibility(View.GONE);
            }
            if (!isRegisteredWithServer) {
                findViewById(R.id.sectionFMDServer).setVisibility(View.GONE);
            }
        }
        findViewById(R.id.buttonExport).setOnClickListener(this::onExportSettingsClicked);
        findViewById(R.id.buttonExit).setOnClickListener(this::onExitClicked);
        findViewById(R.id.buttonConfirm).setOnClickListener(this::onConfirmClicked);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Settings.writeToUri(this, uri);
            }
        }
    }

    private void onExportSettingsClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.putExtra(Intent.EXTRA_TITLE, IO.settingsFileName);
        intent.setType("*/*");
        startActivityForResult(intent, EXPORT_REQ_CODE);
    }

    private void onExitClicked(View view) {
        // Don't set the UPDATEBOARDING_COMPLETED flag
        finish();
    }

    private void onConfirmClicked(View view) {
        if (isPinSet) {
            Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
            settings.setNow(Settings.SET_PIN, "");
        }
        if (isRegisteredWithServer) {
            // SET_FMD_CRYPT_HPW still contains the old-style hash.
            // Thus we can authenticate one last time using that hash, and this call should succeed.
            FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(this));
            repo.unregister(response -> {
                completeAndContinueToMain();
            }, error -> {
                UnregisterUtil.showUnregisterFailedDialog(this, error, this::completeAndContinueToMain);
            });
        }
    }

    private void completeAndContinueToMain() {
        // settings needs to be instantiated here, else we get race conditions on the file
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        settings.setNow(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED, true);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public static void notifyAboutCryptoRefreshIfRequired(Context context) {
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        boolean alreadyCompleted = (Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED);
        if (alreadyCompleted) return;

        boolean isRegisteredWithServer = settings.checkAccountExists();
        boolean isPinSet = !settings.get(Settings.SET_PIN).equals("");

        if (isRegisteredWithServer || isPinSet) {
            String title = context.getString(R.string.notify_crypto_update_title);
            String text = context.getString(R.string.notify_crypto_update_text);
            Notifications.notify(context, title, text, Notifications.CHANNEL_SECURITY);
        }
    }
}
