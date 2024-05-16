package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.content.Intent;


import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.Logger;

public class AppUpdatedReceiver extends SuperReceiver {

    public static final String APP_UPDATED = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(APP_UPDATED)) {
            Logger.logSession("AppUpdate", "restarted");
            config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT, null);
            config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT_ACTIVE_SINCE, null);
            settings.updateSettings();

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.checkAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
            }
        }
    }

}
