package de.nulide.findmydevice.net;

import android.content.Context;
import com.android.volley.Response;
import org.json.JSONObject;


public class RespHandler implements Response.Listener<JSONObject>{


    private RespListener respListener;

    public RespHandler(DataHandler dataHandler, Context context, JSONObject dataObject, int method, String com, RespListener dataListener) {
        this.respListener = new DefaultATListener(dataHandler, context, dataObject, method, com, dataListener);
    }

    public RespHandler(RespListener respListener){
        this.respListener = respListener;
    }

    @Override
    public void onResponse(JSONObject response) {
        respListener.onResponseReceived(response);
    }

    public void setRespListener(RespListener listener){
        this.respListener = listener;
    }

    public RespListener getRespListener() {
        return respListener;
    }
}
