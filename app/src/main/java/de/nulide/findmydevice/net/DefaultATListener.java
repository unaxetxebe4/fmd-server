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

import de.nulide.findmydevice.utils.PatchedVolley;

public class DefaultATListener implements ATListener{

    private final Context context;
    private final JSONObject dataObject;
    private final String url;
    private final DataListener dataListener;

    public DefaultATListener(Context context, JSONObject dataObject, String url, DataListener dataListener) {
        this.context = context;
        this.dataObject = dataObject;
        this.url = url;
        this.dataListener = dataListener;
    }

    @Override
    public void onATReceived(JSONObject response) {
        if (response.has("Data")) {
            try {
                dataObject.put("IDT", response.get("Data"));
                RequestQueue queue = PatchedVolley.newRequestQueue(context);
                JsonObjectRequest locationPutRequest = new JsonObjectRequest(Request.Method.POST, url, dataObject,
                        response1 -> {
                            if(dataListener != null) {
                                dataListener.onDataReceived(response1, url);
                            }
                            },
                        error -> error.printStackTrace()) {

                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Accept", "application/json");
                        return headers;
                    }

                    @Override
                    public byte[] getBody() {
                        return dataObject.toString().getBytes(StandardCharsets.UTF_8);
                    }
                };
                queue.add(locationPutRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
