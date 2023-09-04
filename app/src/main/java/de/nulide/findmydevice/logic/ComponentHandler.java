package de.nulide.findmydevice.logic;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Handler;

import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.services.GPSTimeOutService;

public class ComponentHandler {

    private Sender sender;
    private Context context;
    private JobService service;
    private JobParameters serviceParams;
    private boolean reschedule;

    private LocationHandler locationHandler;
    private MessageHandler messageHandler;

    public ComponentHandler(Context context, JobService service, JobParameters serviceParams) {
        this.context = context;
        messageHandler = new MessageHandler(this);
        locationHandler = new LocationHandler(this);
        this.service = service;
        this.serviceParams = serviceParams;
        this.reschedule = false;
    }

    public void setReschedule(boolean reschedule){
        this.reschedule = reschedule;
    }

    public Sender getSender() {
        return sender;
    }

    public Context getContext() {
        return context;
    }

    public LocationHandler getLocationHandler() {
        return locationHandler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public void finishJob(){
        if(service != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    service.jobFinished(serviceParams, reschedule);
                    GPSTimeOutService.cancelJob(context);
                }
            }, 10000);
        }
    }
}
