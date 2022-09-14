package de.nulide.findmydevice.receiver;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.UnifiedPush;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.net.DataHandler;
import de.nulide.findmydevice.services.FMDServerCommandService;
import de.nulide.findmydevice.utils.PatchedVolley;


public class PushReceiver extends MessagingReceiver {

    public PushReceiver() {
        super();
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull byte[] message, @NonNull String instance) {
        FMDServerCommandService.scheduleJobNow(
                context);
    }

    @Override
    public void onNewEndpoint(@Nullable Context context, @NotNull String s, @NotNull String s1) {

        DataHandler dataHandler = new DataHandler(context);

        JSONObject dataPackage = new JSONObject();
        try {
            dataPackage.put("IDT", "");
            dataPackage.put("Data", s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dataHandler.run(DataHandler.PUSH, dataPackage, null );

    }

    @Override
    public void onRegistrationFailed(@Nullable Context context, @NotNull String s) {

    }

    @Override
    public void onUnregistered(@Nullable Context context, @NotNull String s) {

    }

    public static void Register(Context c){
        if(UnifiedPush.getDistributors(c, new ArrayList<>()).size() > 0){
            UnifiedPush.registerAppWithDialog(c, "", "", new ArrayList<>(), "");
            new PushReceiver();
        }
    }
}
