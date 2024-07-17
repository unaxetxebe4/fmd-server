package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.Logger;


public class BootReceiver extends SuperReceiver {

    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(BOOT_COMPLETED)) {
            Logger.logSession("AfterBootTest", "passed");
            config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT, null);
            config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT_ACTIVE_SINCE, null);

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.checkAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
                PushReceiver.registerWithUnifiedPush(context);
            }
        }
    }

}
