package de.nulide.findmydevice.receiver;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unifiedpush.android.connector.ConstantsKt;
import org.unifiedpush.android.connector.MessagingReceiver;
import org.unifiedpush.android.connector.UnifiedPush;

import java.util.ArrayList;

import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;
import de.nulide.findmydevice.services.FMDServerCommandDownloadService;


public class PushReceiver extends MessagingReceiver {

    private String TAG = PushReceiver.class.getSimpleName();

    public PushReceiver() {
        super();
    }

    @Override
    public void onMessage(@NonNull Context context, @NonNull byte[] message, @NonNull String instance) {
        Log.i(TAG, "Received push message");
        FMDServerCommandDownloadService.scheduleJobNow(context);
    }

    @Override
    public void onNewEndpoint(@Nullable Context context, @NotNull String endpoint, @NotNull String instance) {
        FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(context));
        repo.registerPushEndpoint(endpoint, (error) -> {
            error.printStackTrace();
        });
    }

    @Override
    public void onRegistrationFailed(@Nullable Context context, @NotNull String s) {
        // do nothing
    }

    @Override
    public void onUnregistered(@Nullable Context context, @NotNull String s) {
        FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(context));
        repo.registerPushEndpoint("", (error) -> {
            error.printStackTrace();
        });
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
