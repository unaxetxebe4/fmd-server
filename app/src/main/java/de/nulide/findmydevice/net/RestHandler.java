package de.nulide.findmydevice.net;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.net.interfaces.ErrorListener;
import de.nulide.findmydevice.net.interfaces.PostListener;
import de.nulide.findmydevice.net.interfaces.ResponseListener;
import de.nulide.findmydevice.utils.PatchedVolley;

public class RestHandler implements ResponseListener, ErrorListener, PostListener {

    public static final String GET_AT = "/requestAccess";
    public static final String COMMAND = "/command";
    public static final String LOCATION = "/location";
    public static final String PICTURE = "/picture";
    public static final String DEVICE = "/device";
    public static final String PUSH = "/push";
    public static final String SALT = "/salt";
    public static final String PRIVKEY = "/key";
    public static final String PUBKEY = "/pubKey";
    public static final String PASSWORD = "/password";
    public static final String VERSION = "/version";


    public static final int DEFAULT_METHOD = Request.Method.PUT;
    public static final int DEFAULT_RESP_METHOD = Request.Method.POST;

    private Context context;
    protected Settings settings;
    private int method;
    private String url;

    private String com;
    private JSONObject jsonObject;
    private ErrorListener errorListener;
    private ResponseListener responseListener;

    private PostListener postListener;

    private RequestQueue queue;

    /**
     * Prepares a rest request
     * @param context Needed for Volley and reading settings
     * @param method The HTTP-Method for the request
     * @param com The Rest Interface to use
     * @param jsonObject The JSONObject to send
     */
    public RestHandler(Context context, int method, String com, JSONObject jsonObject) {
        this.context = context;
        IO.context = context;
        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        url = (String)settings.get(Settings.SET_FMDSERVER_URL);
        queue = PatchedVolley.newRequestQueue(context);
        this.com = com;
        this.method = method;
        this.jsonObject = jsonObject;
        DefaultListener dl = new DefaultListener();
        this.errorListener = dl;
        this.responseListener = dl;
        this.postListener = dl;
    }


    /**
     * Prepare and send the request.
     */
    public void run(){
        JsonObjectRequest request = new JsonObjectRequest(method, url + com,
                jsonObject, this, this);
        queue.add(request);
    }

    /**
     * Create an ATRequest and send it.
     * The ATRequest will send this request afterwards.
     */
    public void runWithAT(){
        ATHandler atHandler = new ATHandler(context, this);
        atHandler.run();
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    @Override
    public void onResponse(JSONObject response) {
        responseListener.onResponse(response);
        postListener.onRestFinished(true);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        errorListener.onErrorResponse(error);
        postListener.onRestFinished(false);
    }

    @Override
    public void onRestFinished(boolean success) {

    }

    public PostListener getPostListener() {
        return postListener;
    }

    public void setPostListener(PostListener postListener) {
        this.postListener = postListener;
    }
}
