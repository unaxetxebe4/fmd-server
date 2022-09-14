package de.nulide.findmydevice.services;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.net.DataHandler;
import de.nulide.findmydevice.net.DataListener;
import de.nulide.findmydevice.sender.FooSender;
import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;

public class FMDServerCommandService extends JobService implements DataListener {

    private static final int JOB_ID = 109;
    private Settings settings;
    private JobParameters params;

    @SuppressLint("NewApi")
    @Override
    public boolean onStartJob(JobParameters params) {
        IO.context = this;
        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));

        DataHandler dataHandler = new DataHandler(this);
        dataHandler.run(DataHandler.COMMAND, this);

        this.params = params;


        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @SuppressLint("NewApi")
    public static void scheduleJobNow(Context context) {
        ComponentName serviceComponent = new ComponentName(context, FMDServerCommandService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(0);
        builder.setOverrideDeadline(1000);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public void onDataReceived(JSONObject response, String url) {
        try {
            String command = response.getString("Data");
            if (!command.equals("")) {
                Sender sender = new FooSender();
                Logger.init(Thread.currentThread(), this);
                ComponentHandler ch = new ComponentHandler(settings, this, this, params);
                ch.setSender(sender);
                ch.getLocationHandler().setSendToServer(true);
                ch.getMessageHandler().setSilent(true);
                String fmdCommand = (String)settings.get(Settings.SET_FMD_COMMAND);
                if(command.startsWith("423")){
                    Notifications.init(this, false);
                    Notifications.notify(this, "Serveraccess", "Somebody tried three times in a row to log in the server. Access is locked for 10 minutes", Notifications.CHANNEL_SERVER);
                }else {
                    ch.getMessageHandler().handle(fmdCommand + " " + command, this);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
