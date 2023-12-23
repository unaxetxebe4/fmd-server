package de.nulide.findmydevice.net;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.net.interfaces.ResponseListener;

public class ATHandler extends RestHandler {

    private final RestHandler nextRequest;


    /**
     * Prepare the ATRequest
     * Use the errorhandler from nextRequest
     * @param context For Volley and reading the settings
     * @param nextRequest The Request to send afterwards
     */
    public ATHandler(Context context, RestHandler nextRequest) {
        super(context, DEFAULT_METHOD, GET_AT, getEmptyDataReq());
        setJsonObject(getDefaultATReq());
        setErrorListener(nextRequest.getErrorListener());
        setResponseListener(this);
        this.nextRequest = nextRequest;
    }

    public JSONObject getDefaultATReq(){
        JSONObject requestAccessObject = new JSONObject();
        try {
            requestAccessObject.put("IDT", settings.get(Settings.SET_FMDSERVER_ID));
            requestAccessObject.put("Data", settings.get(Settings.SET_FMD_CRYPT_HPW));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return requestAccessObject;
    }

    public static JSONObject getEmptyDataReq(){
        JSONObject requestDataObject = new JSONObject();
        try {
            requestDataObject.put("IDT", "");
            requestDataObject.put("Data", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return requestDataObject;
    }

    @Override
    public void onResponse(JSONObject response) {
        if (response.has("Data")) {
            try {
                JSONObject nRJSONObject = nextRequest.getJsonObject();
                nRJSONObject.put("IDT", response.get("Data"));
                nextRequest.setJsonObject(nRJSONObject);
                nextRequest.run();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
