package de.nulide.findmydevice.net;

import org.json.JSONObject;

public interface ATListener {

    void onATReceived(JSONObject response);

}
