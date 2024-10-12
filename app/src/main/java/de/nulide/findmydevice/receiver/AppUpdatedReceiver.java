package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.Logger;

public class AppUpdatedReceiver extends SuperReceiver {

    public static final String APP_UPDATED = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(APP_UPDATED)) {
            Logger.logSession("AppUpdate", "restarted");
            settings.updateSettings();

            TempContactExpiredService.scheduleJob(context, 0);

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.checkAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
            }
        }
    }

}
