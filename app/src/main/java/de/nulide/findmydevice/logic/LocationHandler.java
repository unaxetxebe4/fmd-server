
package de.nulide.findmydevice.logic;

import static de.nulide.findmydevice.utils.Utils.getGeoURI;
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
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.net.FMDServerApiRepoSpec;
import de.nulide.findmydevice.net.FMDServerApiRepository;

public class LocationHandler {

    private Settings settings;
    private ComponentHandler ch;

    private boolean sendToServer;

    public LocationHandler(ComponentHandler ch) {
        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(ch.getContext())).getSettings();
        this.ch = ch;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void newLocation(String provider, String lat, String lon) {
        String msg = String.format("%s: Lat: %s Lon: %s\n%s\n%s",
                provider, lat, lon,
                getGeoURI(lat, lon), getOpenStreetMapLink(lat, lon));
        ch.getSender().sendNow(msg);

        long timeMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();

        settings.set(Settings.SET_LAST_KNOWN_LOCATION_LAT, lat);
        settings.set(Settings.SET_LAST_KNOWN_LOCATION_LON, lon);
        settings.set(Settings.SET_LAST_KNOWN_LOCATION_TIME, timeMillis);

        BatteryManager bm = (BatteryManager) ch.getContext().getSystemService(Context.BATTERY_SERVICE);
        String batLevel = Integer.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).toString();

        if (sendToServer || settings.checkAccountExists()) {
            FMDServerApiRepository repo = FMDServerApiRepository.Companion.getInstance(new FMDServerApiRepoSpec(ch.getContext()));
            repo.sendLocation(provider, lat, lon, batLevel, timeMillis);
        }
    }

    public void sendLastKnownLocation() {
        String lat = (String) settings.get(Settings.SET_LAST_KNOWN_LOCATION_LAT);
        String lon = (String) settings.get(Settings.SET_LAST_KNOWN_LOCATION_LON);
        long time = (long) settings.get(Settings.SET_LAST_KNOWN_LOCATION_TIME);
        Date date = new Date(time);
        String msg = String.format("%s: Lat: %s Lon: %s\nTime: %s\n%s\n%s",
                ch.getContext().getString(R.string.MH_LAST_KNOWN_LOCATION),
                lat, lon, date.toString(),
                getGeoURI(lat, lon), getOpenStreetMapLink(lat, lon));
        ch.getSender().sendNow(msg);
    }

    public void setSendToServer(boolean sendToServer) {
        this.sendToServer = sendToServer;
    }
}
