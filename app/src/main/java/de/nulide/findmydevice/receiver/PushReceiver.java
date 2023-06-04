package de.nulide.findmydevice.receiver;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unifiedpush.android.connector.ConstantsKt;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.UnifiedPush;

import java.util.ArrayList;

import de.nulide.findmydevice.services.FMDServerCommandService;
import de.nulide.findmydevice.services.FMDServerService;


public class PushReceiver extends MessagingReceiver {

    public PushReceiver() {
        super();
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull byte[] message, @NonNull String instance) {
        FMDServerCommandService.scheduleJobNow(context);
    }

    @Override
    public void onNewEndpoint(@Nullable Context context, @NotNull String endpoint, @NotNull String instance) {
        FMDServerService.registerPushWithFmdServer(context, endpoint);
    }

    @Override
    public void onRegistrationFailed(@Nullable Context context, @NotNull String s) {
        // do nothing
    }

    @Override
    public void onUnregistered(@Nullable Context context, @NotNull String s) {
        FMDServerService.registerPushWithFmdServer(context, "");
    }

    public static void registerWithUnifiedPush(Context context) {
        if (isUnifiedPushAvailable(context)) {
            UnifiedPush.registerAppWithDialog(context, ConstantsKt.INSTANCE_DEFAULT, "", new ArrayList<>(), "");
        }
    }

    public static void unregisterWithUnifiedPush(Context context) {
        if (isRegisteredWithUnifiedPush(context)) {
            UnifiedPush.unregisterApp(context, ConstantsKt.INSTANCE_DEFAULT);
        }
    }

    public static boolean isRegisteredWithUnifiedPush(Context context) {
        return !UnifiedPush.getDistributor(context).isEmpty();
    }

    public static boolean isUnifiedPushAvailable(Context context) {
        return UnifiedPush.getDistributors(context, new ArrayList<>()).size() > 0;
    }
}
