package de.nulide.findmydevice.net;

import android.content.Context;
import com.android.volley.Response;
import org.json.JSONObject;


public class ATHandler implements Response.Listener<JSONObject>{


    private ATListener atListener;

    public ATHandler(Context context, JSONObject dataObject, String url, DataListener dataListener) {
        this.atListener = new DefaultATListener(context, dataObject, url, dataListener);
    }

    @Override
    public void onResponse(JSONObject response) {
        atListener.onATReceived(response);
    }

    public void setAtListener(ATListener listener){
        this.atListener = listener;
    }
}
