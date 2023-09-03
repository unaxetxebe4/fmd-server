package de.nulide.findmydevice.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.Calendar;

import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.services.FMDSMSService;
import de.nulide.findmydevice.utils.Logger;

public class SMSReceiver extends SuperReceiver {

    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @SuppressLint("NewApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        init(context);
        if (intent.getAction().equals(SMS_RECEIVED)) {
            Calendar time = Calendar.getInstance();
            time.add(Calendar.SECOND, -2);
            if (time.getTimeInMillis() > ((Long) config.get(ConfigSMSRec.CONF_LAST_USAGE))) {
                Bundle bundle = intent.getExtras();
                SmsMessage[] msgs;
                String format = bundle.getString("format");
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    boolean isVersionM =
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        if (isVersionM) {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        } else {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                        String receiver = msgs[i].getOriginatingAddress();
                        FMDSMSService.scheduleJob(context, receiver, msgs[i].getMessageBody(), time.getTimeInMillis());
                    }
                }
                Calendar now = Calendar.getInstance();
                config.set(ConfigSMSRec.CONF_LAST_USAGE, now.getTime());
            }
        }
        Logger.writeLog();
    }

}
