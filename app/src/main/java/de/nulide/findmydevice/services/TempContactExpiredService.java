package de.nulide.findmydevice.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import de.nulide.findmydevice.data.ConfigSMSRec;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.transports.SmsTransport;
import de.nulide.findmydevice.transports.Transport;
import de.nulide.findmydevice.utils.Logger;

public class TempContactExpiredService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        ConfigSMSRec config = JSONFactory.convertJSONConfig(IO.read(JSONMap.class, IO.SMSReceiverTempData));
        String phoneNumber = (String) config.get(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT);

        IO.context = this;
        Logger.init(Thread.currentThread(), this);

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Transport<String> transport = new SmsTransport(phoneNumber);
            transport.send(this, "FindMyDevice: Pin expired!");
            Logger.logSession("Session expired", phoneNumber);
        }

        config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT, null);
        config.set(ConfigSMSRec.CONF_TEMP_WHITELISTED_CONTACT_ACTIVE_SINCE, null);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, TempContactExpiredService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(10 * 1000 * 60);
        builder.setOverrideDeadline(15 * 1000 * 60);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }
}
