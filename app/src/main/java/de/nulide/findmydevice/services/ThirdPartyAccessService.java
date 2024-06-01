package de.nulide.findmydevice.services;

import android.content.Context;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Calendar;
import java.util.Date;

import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.Allowlist;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.data.io.json.JSONWhiteList;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.sender.FooSender;
import de.nulide.findmydevice.sender.NotificationReply;
import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.Permission;

public class ThirdPartyAccessService extends NotificationListenerService {

    private Settings settings;
    protected Allowlist allowlist;
    protected ConfigSMSRec config;

    protected ComponentHandler ch;
    protected  String DEFAULT_SMS_PACKAGE_NAME = "";

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
        Permission.initValues(context);
        ch = new ComponentHandler(context, null, null);

        DEFAULT_SMS_PACKAGE_NAME = Telephony.Sms.getDefaultSmsPackage(context);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        init(this);
        CharSequence msgCS = sbn.getNotification().extras.getCharSequence("android.text");
        if(sbn.getPackageName().equals(DEFAULT_SMS_PACKAGE_NAME)){
            return;
        }
        if(msgCS != null) {
            NotificationReply sender = new NotificationReply(this, sbn);
            if(sender.canSend()) {
                ch.setSender(sender);
                String msg = msgCS.toString();
                String msgLower = msg.toLowerCase();
                String fmdcommand = (String) settings.get(Settings.SET_FMD_COMMAND);
                if (msgLower.contains(fmdcommand)) {
                    msg = ch.getMessageHandler().checkAndRemovePin(msg);
                    if (msg != null) {
                        ch.getMessageHandler().handle(msg, this);
                        cancelNotification(sbn.getKey());
                    }
                }
            }
            if ((Boolean) settings.get(Settings.SET_FMD_LOW_BAT_SEND)) {
                if (sbn.getPackageName().equals("com.android.systemui")) {
                    if (sbn.getTag().equals("low_battery")) {
                        Long lastTime = (Long) config.get(ConfigSMSRec.CONF_TEMP_BAT_CHECK);
                        Long nowTime = new Date().getTime();
                        config.set(ConfigSMSRec.CONF_TEMP_BAT_CHECK, nowTime);
                        if (lastTime == null || lastTime+60000 < nowTime) {
                            Sender dummySender = new FooSender();
                            Logger.log("BatteryWarning", "Low Battery detected: sending message.");
                            ch.setSender(dummySender);
                            String fmdcommand = (String) settings.get(Settings.SET_FMD_COMMAND);
                            ch.getMessageHandler().handle(fmdcommand + " locate", this);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}
