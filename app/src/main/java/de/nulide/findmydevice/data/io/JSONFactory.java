package de.nulide.findmydevice.data.io;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.LogData;
import de.nulide.findmydevice.data.LogEntry;
import de.nulide.findmydevice.data.io.json.JSONLog;
import de.nulide.findmydevice.data.io.json.JSONLogEntry;
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

    public static LogEntry convertJSONLogEntry(JSONLogEntry jsonLogEntry){
        return new LogEntry(jsonLogEntry.getTime(), jsonLogEntry.getText());
    }

    public static JSONLogEntry convertLogEntry(LogEntry logEntry){
        return new JSONLogEntry(logEntry.getTime(), logEntry.getText());
    }

    public static LogData convertJSONLog(JSONLog jsonLog) {
        LogData temp = new LogData();
        if(jsonLog != null) {
            for(JSONLogEntry jsonLogEntry : jsonLog){
                temp.add(convertJSONLogEntry(jsonLogEntry));
            }
        }
        return temp;
    }

    public static JSONLog convertLogData(LogData logData) {
        JSONLog jsonLog = new JSONLog();
        for(LogEntry logEntry : logData){
            jsonLog.add(convertLogEntry(logEntry));
        }
        return jsonLog;
    }
}
