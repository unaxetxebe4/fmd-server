package de.nulide.findmydevice.net;

import org.json.JSONObject;

public interface RespListener {

    void onResponseReceived(JSONObject response);

}
