package de.nulide.findmydevice.logic.command.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.logic.ComponentHandler;
import de.nulide.findmydevice.services.GPSTimeOutService;
import de.nulide.findmydevice.utils.SecureSettings;

public class GPS implements LocationListener {

    private ComponentHandler ch;
    private LocationManager locationManager;
    private boolean jobFullfilled;

    public GPS(ComponentHandler ch) {
        this.ch = ch;
        locationManager = (LocationManager) ch.getContext().getSystemService(Context.LOCATION_SERVICE);
        jobFullfilled = false;
    }

    public static boolean isGPSOn(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return lm.isLocationEnabled();
        }
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && !jobFullfilled) {
            String provider = location.getProvider();
            String lat = new Double(location.getLatitude()).toString();
            String lon = new Double(location.getLongitude()).toString();
            ch.getLocationHandler().newLocation(provider, lat, lon);
            jobFullfilled = true;
            if ((Integer) ch.getSettings().get(Settings.SET_GPS_STATE) == 2) {
                SecureSettings.turnGPS(ch.getContext(), false);
                ch.getSettings().set(Settings.SET_GPS_STATE, 0);
            }
            ch.finishJob();
        } else {
            locationManager.removeUpdates(this);
            ch.finishJob();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    public void sendGPSLocation() {
        for (String provider : locationManager.getAllProviders()) {
            locationManager.requestLocationUpdates(provider, 1000, 0, this);
        }
        GPSTimeOutService.scheduleJob(ch.getContext());
    }
}
