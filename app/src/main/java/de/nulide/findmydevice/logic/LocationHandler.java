
package de.nulide.findmydevice.logic;

import static de.nulide.findmydevice.utils.Utils.getOpenStreetMapLink;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;

public class LocationHandler {

    private ComponentHandler ch;

    private boolean sendToServer;

    public LocationHandler(ComponentHandler ch) {
        this.ch = ch;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void newLocation(String provider, String lat, String lon) {
        String msg = String.format("%s: Lat: %s Lon: %s\n\n%s",
                provider, lat, lon, getOpenStreetMapLink(lat, lon));
        ch.getSender().sendNow(msg);

        long timeMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();

        ch.getSettings().setNow(Settings.SET_LAST_KNOWN_LOCATION_LAT, lat);
        ch.getSettings().setNow(Settings.SET_LAST_KNOWN_LOCATION_LON, lon);
        ch.getSettings().setNow(Settings.SET_LAST_KNOWN_LOCATION_TIME, timeMillis);

        BatteryManager bm = (BatteryManager) ch.getContext().getSystemService(Context.BATTERY_SERVICE);
        String batLevel = Integer.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).toString();

        if (sendToServer || ch.getSettings().checkAccountExists()) {
            FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(ch.getContext()));
            repo.sendLocation(provider, lat, lon, batLevel, timeMillis);
        }
    }

    public void sendLastKnownLocation() {
        String lat = (String) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_LAT);
        String lon = (String) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_LON);
        long time = (long) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_TIME);
        Date date = new Date(time);
        String msg = String.format("%s: Lat: %s Lon: %s\n\nTime: %s\n\n%s",
                ch.getContext().getString(R.string.MH_LAST_KNOWN_LOCATION),
                lat, lon, date.toString(), getOpenStreetMapLink(lat, lon));
        ch.getSender().sendNow(msg);
    }

    public void setSendToServer(boolean sendToServer) {
        this.sendToServer = sendToServer;
    }
}
