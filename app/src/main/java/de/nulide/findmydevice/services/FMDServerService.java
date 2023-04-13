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

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Calendar;

import de.nulide.findmydevice.data.Keys;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.net.DataHandler;
import de.nulide.findmydevice.net.RespHandler;
import de.nulide.findmydevice.net.RespListener;
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

    public static void sendNewLocation(Context context, Settings settings, String provider, String lat, String lon) {
        PublicKey publicKey = settings.getKeys().getPublicKey();
        RequestQueue queue = PatchedVolley.newRequestQueue(context);
        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        String batLevel = Integer.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).toString();

        final JSONObject locationDataObject = new JSONObject();
        try {
            locationDataObject.put("provider", CypherUtils.encodeBase64(CypherUtils.encryptWithKey(publicKey, provider)));
            locationDataObject.put("date", Calendar.getInstance().getTimeInMillis());
            locationDataObject.put("bat", CypherUtils.encodeBase64(CypherUtils.encryptWithKey(publicKey, batLevel)));
            locationDataObject.put("lon", CypherUtils.encodeBase64(CypherUtils.encryptWithKey(publicKey, lon)));
            locationDataObject.put("lat", CypherUtils.encodeBase64(CypherUtils.encryptWithKey(publicKey, lat)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DataHandler dataHandler = new DataHandler(context);
        dataHandler.run(DataHandler.LOCATION, locationDataObject, null);
    }

    public static void sendPicture(Context context, String picture, String url, String id){
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));

        Keys keys = settings.getKeys();
        if(keys.equals(null)) {
            // TODO: Handle no Keys are returned
            // reinitiate Keys in settings
            return;
        }
        PublicKey publicKey = keys.getPublicKey();

        String password = CypherUtils.generateRandomString(25);
        String encryptedPicture = CypherUtils.encryptWithAES(picture.getBytes(StandardCharsets.UTF_8),password);
        String encryptedPassword = CypherUtils.encodeBase64(CypherUtils.encryptWithKey(publicKey, password));
        String msg = encryptedPassword + "___PICTURE-DATA___" + encryptedPicture;

        final JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("Data", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DataHandler dataHandler = new DataHandler(context);
        dataHandler.run(DataHandler.PICTURE, dataObject, null);
    }

    public static void registerOnServer(Context context, String url, String privKey, String pubKey, String salt, String hashedPW) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("salt", salt);
            jsonObject.put("hashedPassword", hashedPW);
            jsonObject.put("pubkey", pubKey);
            jsonObject.put("privkey", privKey);
        }catch (JSONException e){
            e.printStackTrace();
        }

        DataHandler dataHandler = new DataHandler(context);
        RespHandler respHandler = new RespHandler(response -> {
            try {
                settings.set(Settings.SET_FMDSERVER_ID, response.get("DeviceId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        dataHandler.prepareSingle(DataHandler.DEFAULT_METHOD, DataHandler.DEVICE, jsonObject, respHandler);
        dataHandler.send();
    }

    public static void unregisterOnServer(Context context) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        RequestQueue queue = PatchedVolley.newRequestQueue(context);
        String url = (String)settings.get(Settings.SET_FMDSERVER_URL);
        final JSONObject requestAccessObject = new JSONObject();

        DataHandler dataHandler = new DataHandler(context);
        dataHandler.run(DataHandler.DEVICE,null);
        settings.set(Settings.SET_FMDSERVER_ID, "");
        settings.set(Settings.SET_FMDSERVER_AUTO_UPLOAD, false);
        settings.setNow(Settings.SET_FMDSERVER_UPLOAD_SERVICE, false);
    }

    public static void scheduleJob(Context context, int time) {
        ComponentName serviceComponent = new ComponentName(context, FMDServerService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(((long) time / 2) * 1000 * 60);
        builder.setOverrideDeadline((int)(time * 1000 * 60 * 1.5));
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
    public static void loginOnServer(Context context, String id, String password) {
        DataHandler dataHandler = new DataHandler(context);
        JSONObject req = dataHandler.getEmptyDataReq();
        try {
            req.put("IDT", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RespHandler respHandler = new RespHandler(response -> {
            if (response.has("Data")) {
                try {
                    String hashedPW = CypherUtils.hashWithPKBDF2WithGivenSalt(password, (String) response.get("Data"));
                    req.put("Data", hashedPW);
                    RespHandler respHandlerForAT = new RespHandler(ATResponse -> {
                        if (ATResponse.has("Data")) {
                            try {
                                req.put("IDT", ATResponse.get("Data"));


                                RespHandler respHandlerForKey = new RespHandler(privResponse -> {
                                    if(privResponse.has("Data")){

                                        RespHandler respHandlerForPublicKey = new RespHandler(pubResponse -> {
                                            if(pubResponse.has("Data")){
                                                Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
                                                settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
                                                settings.set(Settings.SET_FMDSERVER_ID, id);
                                                try {
                                                    settings.set(Settings.SET_FMD_CRYPT_PUBKEY, pubResponse.get("Data"));
                                                    settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, privResponse.get("Data"));
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        });
                                        dataHandler.prepareSingle(DataHandler.DEFAULT_METHOD, DataHandler.PUBKEY, req, respHandlerForPublicKey);
                                        dataHandler.send();

                                    }

                                });
                                dataHandler.prepareSingle(DataHandler.DEFAULT_METHOD, DataHandler.PRIVKEY, req, respHandlerForKey);
                                dataHandler.send();



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    dataHandler.prepareSingle(DataHandler.DEFAULT_METHOD, DataHandler.GET_AT, req, respHandlerForAT);
                    dataHandler.send();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        dataHandler.prepareSingle(DataHandler.DEFAULT_METHOD, DataHandler.SALT, req, respHandler);
        dataHandler.send();
    }

    public static void changePassword(Context context, String newPrivKey, String salt, String hashedPW) {
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        final JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("salt", salt);
            jsonObject.put("hashedPassword", hashedPW);
            jsonObject.put("privkey", newPrivKey);
        }catch (JSONException e){
            e.printStackTrace();
        }

        DataHandler dataHandler = new DataHandler(context);

        dataHandler.run(DataHandler.PASSWORD, jsonObject, response -> {
            if(response.has("data")){
                settings.set(Settings.SET_FMD_CRYPT_PRIVKEY, newPrivKey);
                settings.set(Settings.SET_FMD_CRYPT_HPW, hashedPW);
            }
        });
        
    }


    @Override
    public boolean onStartJob(JobParameters params) {

        Sender sender = new FooSender();
        IO.context = this;
        Logger.init(Thread.currentThread(), this);
        Logger.logSession("FMDServerService", "started");
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        if ((Boolean) settings.get(Settings.SET_FMDSERVER_UPLOAD_SERVICE)) {

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
        scheduleJob(this, (Integer)settings.get(Settings.SET_FMDSERVER_UPDATE_TIME));
        return false;
    }
}
