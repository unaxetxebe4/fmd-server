package de.nulide.findmydevice.receiver;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.UnifiedPush;

import java.util.ArrayList;

import de.nulide.findmydevice.net.RestHandler;
import de.nulide.findmydevice.services.FMDServerCommandService;


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

        JSONObject dataPackage = new JSONObject();
        try {
            dataPackage.put("IDT", "");
            dataPackage.put("Data", s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestHandler dataHandler = new RestHandler(context, RestHandler.DEFAULT_METHOD, RestHandler.PUSH, dataPackage);
        dataHandler.runWithAT();
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
