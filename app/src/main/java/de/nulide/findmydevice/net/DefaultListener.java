package de.nulide.findmydevice.net;

import com.android.volley.VolleyError;

import org.json.JSONObject;

import de.nulide.findmydevice.net.interfaces.ErrorListener;
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.net.interfaces.ResponseListener;

public class DefaultListener implements ResponseListener, ErrorListener, PostListener {
    @Override
    public void onErrorResponse(VolleyError error) {

    }

    @Override
    public void onResponse(JSONObject response) {

    }

    @Override
    public void onRestFinished(boolean success) {

    }
}
