package de.nulide.findmydevice.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import de.nulide.findmydevice.commands.CommandHandler;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.transports.FmdServerTransport;
import de.nulide.findmydevice.transports.Transport;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import kotlin.Unit;


/**
 * Uploads the location at regular intervals in the background
 */
public class FMDServerLocationUploadService extends FmdJobService {

    private static final String TAG = FMDServerLocationUploadService.class.getSimpleName();

    private static final int JOB_ID = 108;

    public static void scheduleJob(Context context, long delayMinutes) {
        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(context)).getSettings();
        if (((Integer) settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE)) == 3) {
            // user requested NOT to upload any location at regular intervals
            Log.d(TAG, "Not scheduling job. Reason: user requested no upload");
            return;
        }

        ComponentName serviceComponent = new ComponentName(context, FMDServerLocationUploadService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        // We cannot use setPeriodic() because that only works for periods >= 15 mins
        // builder.setPeriodic(intervalMinutes * 60 * 1000);
        // Instead we use setMinimumLatency() and setOverrideDeadline()
        // We add 2 mins to give the system some scheduling flexibility
        builder.setMinimumLatency(delayMinutes * 60 * 1000);
        builder.setOverrideDeadline(((delayMinutes + 2) * 60 * 1000));

        builder.setPersisted(true);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        super.onStartJob(params);

        Log.d(TAG, "Starting background upload job");
        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        IO.context = this;
        Logger.init(Thread.currentThread(), this);
        Notifications.init(this, true);

        Transport<Unit> transport = new FmdServerTransport(this);
        CommandHandler<Unit> commandHandler = new CommandHandler<>(transport, this.getCoroutineScope(), this);

        if (!settings.checkAccountExists()) {
            Logger.logSession(TAG, "No account, stopping and cancelling job.");
            cancelJob(this);
            return false;
        }

        String locateCommand = settings.get(Settings.SET_FMD_COMMAND) + " locate";
        switch ((Integer) settings.get(Settings.SET_FMDSERVER_LOCATION_TYPE)) {
            case 0:
                locateCommand += " gps";
                break;
            case 1:
                locateCommand += " cell";
                break;
            case 2:
                // no need to change the command
                break;
            case 3:
                // we should not be here...
                return false;
        }
        commandHandler.execute(this, locateCommand);

        Logger.logSession(TAG, "command issued, waiting for location");
        Logger.writeLog();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        super.onStopJob(params);
        // request retry (instead of scheduleNextOccurrence())
        return true;
    }

    @Override
    public void jobFinished() {
        super.jobFinished();
        scheduleNextOccurrence();
    }

    private void scheduleNextOccurrence() {
        Logger.log(TAG, "job stopped, rescheduling");
        Log.d(TAG, "job stopped, rescheduling");
        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();

        long intervalMinutes = ((Integer) settings.get(Settings.SET_FMDSERVER_UPDATE_TIME)).longValue();
        if (intervalMinutes <= 0) {
            Log.i(TAG, "Raising interval from " + intervalMinutes + " mins to 1 min");
            intervalMinutes = 1;
        }

        scheduleJob(this, intervalMinutes);
    }
}
