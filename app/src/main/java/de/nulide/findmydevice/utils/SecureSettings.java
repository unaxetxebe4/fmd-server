package de.nulide.findmydevice.utils;

import android.content.Context;
import android.provider.Settings;

public class SecureSettings {

    public static void turnGPS(Context context, boolean enable) {
        int value;
        if (enable) {
            value = android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        } else {
            value = android.provider.Settings.Secure.LOCATION_MODE_OFF;
        }
        Settings.Secure.putString(context.getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE, Integer.valueOf(value).toString());
    }

}
