package de.nulide.findmydevice.services;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.BatteryManager;

import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.PublicKey;
import java.util.Calendar;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.FmdKeyPair;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.net.ATHandler;
import de.nulide.findmydevice.net.RestHandler;
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.sender.FooSender;
import de.nulide.findmydevice.sender.Sender;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.PatchedVolley;
import de.nulide.findmydevice.utils.Permission;


@SuppressLint("NewApi")
public class FMDServerService extends JobService {

    private static final int JOB_ID = 108;

    public static void sendNewLocation(Context context, Settings settings, String provider, String lat, String lon, String time) {
        PublicKey publicKey = settings.getKeys().getPublicKey();
        RequestQueue queue = PatchedVolley.newRequestQueue(context);

        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        String batLevel = Integer.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).toString();

        final JSONObject locationDataObject = new JSONObject();
        try {
            locationDataObject.put("provider", provider);
            locationDataObject.put("date", Calendar.getInstance().getTimeInMillis());
            locationDataObject.put("bat", batLevel);
            locationDataObject.put("lon", lon);
            locationDataObject.put("lat", lat);
            locationDataObject.put("time", time);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsonSerialised = locationDataObject.toString();
        byte[] encryptedLocationBytes = CypherUtils.encryptWithKey(publicKey, jsonSerialised);
        String encryptedLocation = CypherUtils.encodeBase64(encryptedLocationBytes);

        final JSONObject encryptedLocationDataObject = new JSONObject();
        try {
            encryptedLocationDataObject.put("Data", encryptedLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestHandler restHandler = new RestHandler(context, RestHandler.DEFAULT_RESP_METHOD, RestHandler.LOCATION, encryptedLocationDataObject);
        restHandler.runWithAT();
    }

    public static void sendPicture(Context context, String picture, String url, String id) {
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));

        FmdKeyPair keys = settings.getKeys();
        if (keys.equals(null)) {
            // TODO: Handle no Keys are returned
            // reinitiate Keys in settings
            return;
        }

        byte[] msgBytes = CypherUtils.encryptWithKey(keys.getPublicKey(), picture);
        String msg = CypherUtils.encodeBase64(msgBytes);

        final JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("Data", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestHandler restHandler = new RestHandler(context, RestHandler.DEFAULT_RESP_METHOD, RestHandler.PICTURE, dataObject);
        restHandler.runWithAT();
    }

    public static void registerOnServer(Context context, String url, String privKey, String pubKey, String hashedPW, PostListener postListener) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("hashedPassword", hashedPW);
            jsonObject.put("pubkey", pubKey);
            jsonObject.put("privkey", privKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RestHandler restHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.DEVICE, jsonObject);
        restHandler.setResponseListener(response -> {
            try {
                settings.setNow(Settings.SET_FMDSERVER_ID, response.get("DeviceId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        restHandler.setPostListener(postListener);
        restHandler.run();
    }

    public static void unregisterOnServer(Context context) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));

        RestHandler restHandler = new RestHandler(context, RestHandler.DEFAULT_RESP_METHOD, RestHandler.DEVICE, ATHandler.getEmptyDataReq());
        restHandler.runWithAT();
        settings.setNow(Settings.SET_FMDSERVER_ID, "");
    }

    public static void scheduleJob(Context context, int time) {
        ComponentName serviceComponent = new ComponentName(context, FMDServerService.class);
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

    //First get salt from Server
    //Second gen hashedpw and send it to server for AccessToken
    //Third Get PrivateKey
    //Fourth Get PublicKey
    //Fifth Save everything
    public static void loginOnServer(Context context, String id, String password, PostListener postListener) {
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        JSONObject req = ATHandler.getEmptyDataReq();
        try {
            req.put("IDT", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestHandler saltHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.SALT, req);
        saltHandler.setResponseListener(response -> {
            if (response.has("Data")) {
                    try {
                        String salt = (String) response.get("Data");
                        String hashedPW = CypherUtils.hashPasswordForLogin(password, salt);
                        req.put("Data", hashedPW);
                        RestHandler atHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.GET_AT, req);
                        atHandler.setResponseListener(atResponse -> {
                            if (atResponse.has("Data")) {
                            try {
                                req.put("IDT", (String)atResponse.get("Data"));
                                RestHandler privKeyHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.PRIVKEY, req);
                                privKeyHandler.setResponseListener(privResponse -> {
                                    if (privResponse.has("Data")) {
                                        RestHandler pubKeyHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.PUBKEY, req);
                                        pubKeyHandler.setResponseListener(pubResponse -> {
                                            if (pubResponse.has("Data")) {
                                                try {
                                                    settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
                                                    settings.set(Settings.SET_FMDSERVER_ID, id);
                                                    settings.set(Settings.SET_FMD_CRYPT_PUBKEY, pubResponse.get("Data"));
                                                    settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, privResponse.get("Data"));
                                                }catch (JSONException e){
                                                    e.printStackTrace();
                                                }

                                            }
                                        });
                                        pubKeyHandler.setPostListener(postListener);
                                        pubKeyHandler.run();

                                    }
                                });
                                privKeyHandler.run();

                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    });
                    atHandler.run();
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                }

        });
        saltHandler.run();
    }

    public static void changePassword(Context context, String newPrivKey, String hashedPW, PostListener postListener) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("hashedPassword", hashedPW);
            jsonObject.put("privkey", newPrivKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RestHandler restHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.PASSWORD, jsonObject);
        restHandler.setPostListener(postListener);
        restHandler.setResponseListener(response -> {
            if (response.has("Data")) {
                settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, newPrivKey);
                settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
            }
        });
        restHandler.runWithAT();
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
