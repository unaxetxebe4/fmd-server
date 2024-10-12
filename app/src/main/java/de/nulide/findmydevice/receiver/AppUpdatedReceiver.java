package de.nulide.findmydevice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.FmdLogKt;

public class AppUpdatedReceiver extends BroadcastReceiver {

    private static final String TAG = AppUpdatedReceiver.class.getSimpleName();

    public static final String APP_UPDATED = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);

        if (intent.getAction().equals(APP_UPDATED)) {
            FmdLogKt.log(context).i(TAG, "Running MY_PACKAGE_REPLACED (APP_UPDATED) handler");

            TempContactExpiredService.scheduleJob(context, 0);

            settings.migrateSettings();
            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.serverAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
            }
        }
    }

}
