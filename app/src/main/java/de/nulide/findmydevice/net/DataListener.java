package de.nulide.findmydevice.net;

import org.json.JSONObject;

public interface DataListener {

    void onDataReceived(JSONObject response, String url);
}
