package de.nulide.findmydevice.services;

import android.content.Context;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.nulide.findmydevice.commands.CommandHandler;
import de.nulide.findmydevice.data.Allowlist;
import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.data.io.json.JSONWhiteList;
import de.nulide.findmydevice.transports.FmdServerTransport;
import de.nulide.findmydevice.transports.NotificationReplyTransport;
import de.nulide.findmydevice.transports.Transport;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import kotlin.Unit;


public class ThirdPartyAccessService extends NotificationListenerService {

    private Settings settings;
    protected Allowlist allowlist;
    protected ConfigSMSRec config;

    protected void init(Context context) {
        IO.context = context;
        Logger.init(Thread.currentThread(), context);

        allowlist = JSONFactory.convertJSONWhiteList(IO.read(JSONWhiteList.class, IO.whiteListFileName));
        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        config = JSONFactory.convertJSONConfig(IO.read(JSONMap.class, IO.SMSReceiverTempData));

        if (config.get(ConfigSMSRec.CONF_LAST_USAGE) == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -5);
            config.set(ConfigSMSRec.CONF_LAST_USAGE, cal.getTimeInMillis());
        }
        Notifications.init(context, false);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        init(this);

        // SMS is handled separately
        if (sbn.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            return;
        }

        if ((Boolean) settings.get(Settings.SET_FMD_LOW_BAT_SEND)) {
            if (sbn.getPackageName().equals("com.android.systemui")) {
                if (sbn.getTag().equals("low_battery")) {
                    handleLowBatteryNotification();
                    return;
                }
            }
        }

        CharSequence messageChars = sbn.getNotification().extras.getCharSequence("android.text");
        if (messageChars == null) {
            return;
        }
        String message = messageChars.toString().toLowerCase(Locale.ROOT);

        String fmdTriggerWord = (String) settings.get(Settings.SET_FMD_COMMAND);
        if (message.contains(fmdTriggerWord)) {
            message = CommandHandler.checkAndRemovePin(settings, message);
            if (message == null){
                // TODO: wrong PIN!
                return;
            }

            Transport<StatusBarNotification> transport = new NotificationReplyTransport(sbn);
            CommandHandler<StatusBarNotification> commandHandler = new CommandHandler<>(transport, null);
            commandHandler.execute(this, message);

            cancelNotification(sbn.getKey());
        }
    }

    // TODO: maybe rename this service to better reflect that it handles this as well?
    private void handleLowBatteryNotification() {
        Long lastTime = (Long) config.get(ConfigSMSRec.CONF_TEMP_BAT_CHECK);
        long nowTime = new Date().getTime();
        config.set(ConfigSMSRec.CONF_TEMP_BAT_CHECK, nowTime);

        if (lastTime == null || lastTime + 60000 < nowTime) {
            Logger.log("BatteryWarning", "Low Battery detected: sending message.");

            Transport<Unit> transport = new FmdServerTransport(this);
            CommandHandler<Unit> commandHandler = new CommandHandler<>(transport, null);

            String locateCommand = settings.get(Settings.SET_FMD_COMMAND) + " locate";
            commandHandler.execute(this, locateCommand);
        }
    }
}
