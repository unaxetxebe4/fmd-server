package de.nulide.findmydevice.net;

import android.content.Context;
import com.android.volley.Response;
import org.json.JSONObject;


public class RespHandler implements Response.Listener<JSONObject>{


    private RespListener atListener;

    public RespHandler(DataHandler dataHandler, Context context, JSONObject dataObject, int method, String com, RespListener dataListener) {
        this.atListener = new DefaultATListener(dataHandler, context, dataObject, method, com, dataListener);
    }

    @Override
    public void onResponse(JSONObject response) {
        atListener.onResponseReceived(response);
    }

    public void setAtListener(RespListener listener){
        this.atListener = listener;
    }

    public RespListener getAtListener() {
        return atListener;
    }
}
