package de.nulide.findmydevice.utils;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.VolleyError;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;

public class UnregisterUtil {

    public static void showUnregisterFailedDialog(Context context, VolleyError error, OnContinueClickedListener onContinueClickedListener) {

        String message = context.getString(R.string.server_unregister_failed_body);
        if (error != null && error.getMessage() != null) {
            message = message.replace("{ERROR}", error.getMessage());
        } else {
            message = message.replace("{ERROR}", "error or getMessage() was null!");
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.server_unregister_failed_title))
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.server_unregister_continue_anyway),
                        (DialogInterface dialog, int which) -> {
                            Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
                            settings.setNow(Settings.SET_FMDSERVER_ID, ""); // force local logout
                            onContinueClickedListener.onContinueClicked();
                            dialog.dismiss();
                        })
                .setNegativeButton(context.getString(R.string.server_unregister_dont_continue),
                        (DialogInterface dialog, int which) -> {
                            dialog.dismiss();
                        })
                .show();
    }

    public interface OnContinueClickedListener {
        void onContinueClicked();
    }

}
