package de.nulide.findmydevice.net;


import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.services.FMDServerCommandService;
import de.nulide.findmydevice.utils.PatchedVolley;

public class DataHandler {

    private static final String GET_AT = "/requestAccess";
    public static final String COMMAND = "/command";
    public static final String  LOCATION = "/location";
    public static final String PICTURE = "/picture";
    public static final String DEVICE = "/device";

    private Context context;
    private Settings settings;
    private String url;
    private RequestQueue queue;

    private JsonObjectRequest request;
    private ATHandler ath;

    public DataHandler(Context context) {
        this.context = context;
        IO.context = context;
        Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        url = (String)settings.get(Settings.SET_FMDSERVER_URL);
        queue = PatchedVolley.newRequestQueue(context);
    }

    public void run(String com, DataListener listener){
        final JSONObject requestDataObject = new JSONObject();
        try {
            requestDataObject.put("IDT", "");
            requestDataObject.put("Data", "");
            run(com, requestDataObject, listener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void run(String com, JSONObject object, DataListener listener){
        final JSONObject requestAccessObject = new JSONObject();
        try {
            requestAccessObject.put("IDT", settings.get(Settings.SET_FMDSERVER_ID));
            requestAccessObject.put("Data", settings.get(Settings.SET_FMD_CRYPT_HPW));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prepare(Request.Method.PUT, com, requestAccessObject, object, listener);
        send();
    }

    public void prepare(int method, String com, JSONObject req, JSONObject object, DataListener listener){

        ath = new ATHandler(context, object, url + com, listener);
        request = new JsonObjectRequest(method, url + GET_AT,
                req, ath,
                error -> error.printStackTrace()) {
            @Override
            public Map<String, String> getHeaders()
            {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() {
                return req.toString().getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    public ATHandler getAth(){
        return ath;
    }

    public void send(){
        queue.add(request);
    }

}
