
package de.nulide.findmydevice.logic;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.services.FMDServerService;

public class LocationHandler {

    private ComponentHandler ch;

    private boolean sendToServer;


    public LocationHandler(ComponentHandler ch){
        this.ch = ch;
    }

    public void newLocation(String provider, String lat, String lon){
        StringBuilder sb = new StringBuilder(provider);
        sb.append(": Lat: ").append(lat).append(" Lon: ").append(lon).append("\n\n").append(createMapLink(lat, lon));
        ch.getSender().sendNow(sb.toString());
        long timeMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
        String time =  (new Date(timeMillis)).toString();

        ch.getSettings().set(Settings.SET_LAST_KNOWN_LOCATION_LAT, lat);
        ch.getSettings().set(Settings.SET_LAST_KNOWN_LOCATION_LON, lon);
        ch.getSettings().set(Settings.SET_LAST_KNOWN_LOCATION_TIME, timeMillis);

        if(sendToServer || ch.getSettings().checkAccountExists()){
            String id =  (String) ch.getSettings().get(Settings.SET_FMDSERVER_ID);
            if(!id.isEmpty()) {
                FMDServerService.sendNewLocation(ch.getContext(), ch.getSettings(), provider, lat, lon, time);
            }
        }
    }

    public void sendLastKnownLocation(){
        String lat = (String) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_LAT);
        String lon = (String) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_LON);
        long time = (long) ch.getSettings().get(Settings.SET_LAST_KNOWN_LOCATION_TIME);
        Date date = new Date(time);
        StringBuilder sb = new StringBuilder(ch.getContext().getString(R.string.MH_LAST_KNOWN_LOCATION));
        sb.append(": Lat: ").append(lat).append(" Lon: ").append(lon).append("\n\n").append("Time: ").append(date.toString()).append("\n\n").append(createMapLink(lat.toString(), lon.toString()));
        ch.getSender().sendNow(sb.toString());
    }

    private String createMapLink(String lat, String lon){
        StringBuilder link = new StringBuilder("https://www.openstreetmap.org/?mlat=");
        link.append(lat).append("&mlon=").append(lon).append("#map=14/").append(lat).append("/").append(lon);
        return link.toString();
    }

    public void setSendToServer(boolean sendToServer) {
        this.sendToServer = sendToServer;
    }
}
