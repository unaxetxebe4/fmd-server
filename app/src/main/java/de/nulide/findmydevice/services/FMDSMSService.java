package de.nulide.findmydevice.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.PhoneNumberUtils;

import java.util.Calendar;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.commands.CommandHandler;
import de.nulide.findmydevice.data.Allowlist;
import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.data.Contact;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.data.io.json.JSONWhiteList;
import de.nulide.findmydevice.transports.SmsTransport;
import de.nulide.findmydevice.transports.Transport;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;


public class FMDSMSService extends FmdJobService {

    private static final String TAG = FMDSMSService.class.getSimpleName();

    private static final int JOB_ID = 107;

    private static final String DESTINATION = "dest";
    private static final String MESSAGE = "msg";
    private static final String TIME = "time";

    private Settings settings;

    public static void scheduleJob(Context context, String destination, String message, Long time) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(DESTINATION, destination);
        bundle.putString(MESSAGE, message);
        bundle.putLong(TIME, time);

        ComponentName serviceComponent = new ComponentName(context, FMDSMSService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                .setExtras(bundle);
        builder.setMinimumLatency(0);
        builder.setOverrideDeadline(0);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    public boolean onStartJob(JobParameters params) {
        super.onStartJob(params);

        IO.context = this;
        Logger.init(Thread.currentThread(), this);

        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        Allowlist allowlist = JSONFactory.convertJSONWhiteList(IO.read(JSONWhiteList.class, IO.whiteListFileName));
        ConfigSMSRec config = JSONFactory.convertJSONConfig(IO.read(JSONMap.class, IO.SMSReceiverTempData));

        if (config.get(ConfigSMSRec.CONF_LAST_USAGE) == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -5);
            config.set(ConfigSMSRec.CONF_LAST_USAGE, cal.getTimeInMillis());
        }
        Notifications.init(this, false);

        String phoneNumber = params.getExtras().getString(DESTINATION);
        String msg = params.getExtras().getString(MESSAGE);
        Long time = params.getExtras().getLong(TIME);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Logger.logSession(TAG, "Cannot handle SMS: phoneNumber is empty!");
            return false;
        }
        if (msg == null || msg.isEmpty()) {
            Logger.logSession(TAG, "Cannot handle SMS: msg is empty!");
            return false;
        }

        Transport<String> transport = new SmsTransport(phoneNumber);
        CommandHandler<String> commandHandler = new CommandHandler<>(transport, this);

        // Case 1: phone number in Allowed Contacts
        for (Contact c : allowlist) {
            if (PhoneNumberUtils.compare(c.getNumber(), phoneNumber)) {
                Logger.logSession(TAG, phoneNumber + " used FMD via allowlist");
                commandHandler.execute(this, msg);
                return true;
            }
        }

        // Case 2: phone number in temporary allowlist (i.e., it send the correct PIN earlier)
        if ((Boolean) settings.get(Settings.SET_ACCESS_VIA_PIN) && !((String) settings.get(Settings.SET_PIN)).isEmpty()) {
            String tempContact = (String) config.get(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT);
            if (tempContact != null && PhoneNumberUtils.compare(tempContact, phoneNumber)) {
                Logger.logSession(TAG, phoneNumber + " used FMD via temporary allowlist");
                commandHandler.execute(this, msg);
                return true;
            }

            // Case 3: the message contains the correct PIN
            if (CommandHandler.checkAndRemovePin(settings, msg) != null) {
                Logger.logSession(TAG, phoneNumber + " used FMD via PIN");
                transport.send(this, getString(R.string.MH_Pin_Accepted));
                Notifications.notify(this, "Pin", "The pin was used by the following number: " + phoneNumber + "\nPlease change the Pin!", Notifications.CHANNEL_PIN);

                config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT, phoneNumber);
                config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT_ACTIVE_SINCE, time);
                TempContactExpiredService.scheduleJob(this);

                // TODO: Execute command directly if the message contains one
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        super.onStopJob(params);
        return false;
    }
}
