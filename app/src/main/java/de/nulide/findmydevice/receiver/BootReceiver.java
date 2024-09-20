package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.Logger;


public class BootReceiver extends SuperReceiver {

    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(BOOT_COMPLETED)) {
            Logger.logSession("AfterBootTest", "passed");

            TempContactExpiredService.scheduleJob(context, 0);

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.checkAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
                PushReceiver.registerWithUnifiedPush(context);
            }
        }
    }

}
