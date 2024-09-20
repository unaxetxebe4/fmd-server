package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.FmdLogKt;

public class AppUpdatedReceiver extends SuperReceiver {

    private static final String TAG = AppUpdatedReceiver.class.getSimpleName();

    public static final String APP_UPDATED = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(APP_UPDATED)) {
            FmdLogKt.log(context).i(TAG, "Running MY_PACKAGE_REPLACED (APP_UPDATED) handler");
            settings.updateSettings();

            TempContactExpiredService.scheduleJob(context, 0);

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.checkAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
            }
        }
    }

}
