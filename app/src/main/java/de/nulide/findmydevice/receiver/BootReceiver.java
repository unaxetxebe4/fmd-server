package de.nulide.findmydevice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.FmdBatteryLowService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.utils.FmdLogKt;


public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getSimpleName();

    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);

        if (intent.getAction().equals(BOOT_COMPLETED)) {
            FmdLogKt.log(context).i(TAG, "Running BOOT_COMPLETED handler");

            TempContactExpiredService.scheduleJob(context, 0);

            if ((Boolean)settings.get(Settings.SET_FMD_LOW_BAT_SEND)){
                FmdBatteryLowService.scheduleJobNow(context);
            }

            UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context);

            if (settings.serverAccountExists()) {
                FMDServerLocationUploadService.scheduleJob(context, 0);
                PushReceiver.registerWithUnifiedPush(context);
            }
        }
    }

}
