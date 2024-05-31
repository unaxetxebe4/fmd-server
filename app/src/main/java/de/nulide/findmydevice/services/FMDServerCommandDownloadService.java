package de.nulide.findmydevice.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.sender.FooSender;
import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;

/**
 * Downloads the latest command and executes it
 */
public class FMDServerCommandDownloadService extends JobService {

    private String TAG = FMDServerCommandDownloadService.class.getSimpleName();

    private static final int JOB_ID = 109;
    private Settings settings;
    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        this.params = params;

        FMDServerApiRepository fmdServerRepo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(this));
        fmdServerRepo.getCommand(this::onResponse, Throwable::printStackTrace);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleJobNow(Context context) {
        ComponentName serviceComponent = new ComponentName(context, FMDServerCommandDownloadService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(0);
        builder.setOverrideDeadline(1000);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    private void onResponse(String remoteCommand) {
        Log.i(TAG, "Received remote command '" + remoteCommand + "'");
        if (remoteCommand.equals("")) {
            return;
        }
        if (remoteCommand.startsWith("423")) {
            Notifications.init(this, false);
            Notifications.notify(this, "Serveraccess", "Somebody tried three times in a row to log in the server. Access is locked for 10 minutes", Notifications.CHANNEL_SERVER);
            return;
        }
        Sender sender = new FooSender();
        Logger.init(Thread.currentThread(), this);
        ComponentHandler ch = new ComponentHandler(this, this, params);
        ch.setSender(sender);
        ch.getLocationHandler().setSendToServer(true);
        ch.getMessageHandler().setSilent(true);
        String fmdCommand = (String) settings.get(Settings.SET_FMD_COMMAND);

        ch.getMessageHandler().handle(fmdCommand + " " + remoteCommand, this);
    }
}
