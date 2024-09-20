package de.nulide.findmydevice.data.io;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.json.JSONMap;

public class JSONFactory {

    public static Settings convertJSONSettings(JSONMap jsonSettings) {
        Settings settings = new Settings();
        if(jsonSettings != null) {
            settings.putAll(jsonSettings);
        }
        return settings;
    }

    public static JSONMap convertSettings(Settings settings) {
        JSONMap jsonSettings = new JSONMap();
        jsonSettings.putAll(settings);
        return jsonSettings;
    }

}
