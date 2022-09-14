package de.nulide.findmydevice.net;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

public class DefaultATListener implements RespListener {

    private final Context context;
    private final JSONObject dataObject;
    private final String com;
    private final RespListener dataListener;
    private final DataHandler dataHandler;
    private final int method;

    public DefaultATListener(DataHandler dataHandler, Context context, JSONObject dataObject, int method, String com, RespListener dataListener) {
        this.context = context;
        this.dataObject = dataObject;
        this.com = com;
        this.dataListener = dataListener;
        this.dataHandler = dataHandler;
        this.method = method;
    }

    @Override
    public void onResponseReceived(JSONObject response) {
        if (response.has("Data")) {
            try {
                dataObject.put("IDT", response.get("Data"));
                dataHandler.prepare(method, 0, com, dataObject, null, null);
                dataHandler.send();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
