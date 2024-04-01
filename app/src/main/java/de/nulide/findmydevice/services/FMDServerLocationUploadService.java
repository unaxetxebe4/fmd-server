package de.nulide.findmydevice.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.sender.FooSender;
import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.Permission;

/**
 * Uploads the location at regular intervals in the background
 */
@RequiresApi(Build.VERSION_CODES.M)
public class FMDServerLocationUploadService extends JobService {

    private static final int JOB_ID = 108;

    public static void scheduleJob(Context context, int time) {
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        ComponentHandler ch = new ComponentHandler(settings, context, null, null);
        if (((Integer) ch.getSettings().get(Settings.SET_FMDSERVER_LOCATION_TYPE)) == 3) {
            // user requested NOT to upload any location at regular intervals
            return;
        }

        ComponentName serviceComponent = new ComponentName(context, FMDServerLocationUploadService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(((long) time / 2) * 1000 * 60);
        builder.setOverrideDeadline((int) (time * 1000 * 60 * 1.5));
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
        Logger.logSession("FMDServerService", "scheduled new job");
    }

    public static void cancelAll(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancelAll();
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        Sender sender = new FooSender();
        IO.context = this;
        Logger.init(Thread.currentThread(), this);
        Logger.logSession("FMDServerService", "started");
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        if (settings.checkAccountExists()) {

            ComponentHandler ch = new ComponentHandler(settings, this, this, params);
            ch.setSender(sender);
            ch.setReschedule(true);
            boolean registered = !((String) ch.getSettings().get(Settings.SET_FMDSERVER_ID)).isEmpty();
            if (registered) {
                Notifications.init(this, true);
                Permission.initValues(this);
                ch.getLocationHandler().setSendToServer(true);
                ch.getMessageHandler().setSilent(true);
                String locateCommand = " locate";
                switch ((Integer) ch.getSettings().get(Settings.SET_FMDSERVER_LOCATION_TYPE)) {
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
                        return true;
                }
                ch.getMessageHandler().handle(ch.getSettings().get(Settings.SET_FMD_COMMAND) + locateCommand, this);
            }
            Logger.logSession("FMDServerService", "finished job, waiting for location");
            Logger.writeLog();

            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Logger.log("FMDServerService", "job stopped by system");
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        scheduleJob(this, (Integer) settings.get(Settings.SET_FMDSERVER_UPDATE_TIME));
        return false;
    }
}
