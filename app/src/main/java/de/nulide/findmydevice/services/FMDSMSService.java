package de.nulide.findmydevice.services;

import static de.nulide.findmydevice.data.TemporaryAllowlistRepositoryKt.TEMP_USAGE_VALIDITY_MILLIS;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.commands.CommandHandler;
import de.nulide.findmydevice.data.AllowlistRepository;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.TemporaryAllowlistRepository;
import de.nulide.findmydevice.transports.SmsTransport;
import de.nulide.findmydevice.transports.Transport;
import de.nulide.findmydevice.utils.FmdLogKt;
import de.nulide.findmydevice.utils.Notifications;


public class FMDSMSService extends FmdJobService {

    private static final String TAG = FMDSMSService.class.getSimpleName();

    private static final int JOB_ID = 107;

    private static final String DESTINATION = "dest";
    private static final String MESSAGE = "msg";

    public static void scheduleJob(Context context, String destination, String message) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(DESTINATION, destination);
        bundle.putString(MESSAGE, message);

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

        SettingsRepository settings = SettingsRepository.Companion.getInstance(this);
        AllowlistRepository allowlistRepo = AllowlistRepository.Companion.getInstance(this);
        TemporaryAllowlistRepository tempAllowlistRepo = TemporaryAllowlistRepository.Companion.getInstance(this);

        String phoneNumber = params.getExtras().getString(DESTINATION);
        String msg = params.getExtras().getString(MESSAGE);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            FmdLogKt.log(this).i(TAG, "Cannot handle SMS: phoneNumber is empty!");
            return false;
        }
        if (msg == null || msg.isEmpty()) {
            FmdLogKt.log(this).i(TAG, "Cannot handle SMS: msg is empty!");
            return false;
        }
        String fmdTriggerWord = (String) settings.get(Settings.SET_FMD_COMMAND);
        if (!msg.contains(fmdTriggerWord)) {
            return false;
        }

        Transport<String> transport = new SmsTransport(this, phoneNumber);
        CommandHandler<String> commandHandler = new CommandHandler<>(transport, this.getCoroutineScope(), this);

        // Case 1: phone number in Allowed Contacts
        if (allowlistRepo.containsNumber(phoneNumber)) {
            FmdLogKt.log(this).i(TAG, phoneNumber + " used FMD via allowlist");
            commandHandler.execute(this, msg);
            return true;
        }

        // Case 2: phone number in temporary allowlist (i.e., it send the correct PIN earlier)
        if ((Boolean) settings.get(Settings.SET_ACCESS_VIA_PIN) && !((String) settings.get(Settings.SET_PIN)).isEmpty()) {

            if (tempAllowlistRepo.containsValidNumber(phoneNumber)) {
                FmdLogKt.log(this).i(TAG, phoneNumber + " used FMD via temporary allowlist");
                commandHandler.execute(this, msg);
                return true;
            }

            // Case 3: the message contains the correct PIN
            if (CommandHandler.checkAndRemovePin(settings, msg) != null) {
                FmdLogKt.log(this).i(TAG, phoneNumber + " used FMD via PIN");
                transport.send(this, getString(R.string.MH_Pin_Accepted));
                Notifications.notify(this, "Pin", "The pin was used by the following number: " + phoneNumber + "\nPlease change the Pin!", Notifications.CHANNEL_PIN);

                tempAllowlistRepo.add(phoneNumber);
                TempContactExpiredService.scheduleJob(this, TEMP_USAGE_VALIDITY_MILLIS + 1000);

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
