package de.nulide.findmydevice.data;


import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.OldKeyIO;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.utils.CypherUtils;
import de.nulide.findmydevice.utils.RingerUtils;


public class Settings extends HashMap<Integer, Object> {

    public static final int settingsVersion = 2;

    public static final int SET_WIPE_ENABLED = 0;
    public static final int SET_ACCESS_VIA_PIN = 1;
    public static final int SET_LOCKSCREEN_MESSAGE = 2;
    public static final int SET_PIN = 3;
    public static final int SET_FMD_COMMAND = 4;
    public static final int SET_OPENCELLID_API_KEY = 5;
    //public static final int SET_INTRODUCTION_VERSION = 6;
    public static final int SET_RINGER_TONE = 7;
    public static final int SET_SET_VERSION = 8;

    //public static final int SET_FMDSERVER_UPLOAD_SERVICE = 101;
    public static final int SET_FMDSERVER_URL = 102;
    public static final int SET_FMDSERVER_UPDATE_TIME = 103;
    public static final int SET_FMDSERVER_ID = 104;
    public static final int SET_FMDSERVER_PASSWORD_SET = 105;
    public static final int SET_FMDSERVER_LOCATION_TYPE = 106; // 0=GPS, 1=CELL, 2=ALL, 3=NONE
    //public static final int SET_FMDSERVER_AUTO_UPLOAD = 107;
    public static final int SET_FMD_CRYPT_PUBKEY = 108;
    public static final int SET_FMD_CRYPT_PRIVKEY = 109;
    public static final int SET_FMD_CRYPT_HPW = 110;
    public static final int SET_FMD_LOW_BAT_SEND = 111;

    //public static final int SET_FMD_CRYPT_NEW_SALT = 112;
    public static final int SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED = 113;


    public static final int SET_FIRST_TIME_WHITELIST = 301;
    public static final int SET_FIRST_TIME_CONTACT_ADDED = 302;
    //public static final int SET_FIRST_TIME_FMD_SERVER = 303;

    public static final int SET_APP_CRASHED_LOG_ENTRY = 401;
    public static final int SET_FMDSMS_COUNTER = 402;

    //public static final int SET_GPS_STATE = 501;         // 0=GPS is off 1=GPS is on 2=GPS is turned on by FMD
    public static final int SET_LAST_KNOWN_LOCATION_LAT = 502;
    public static final int SET_LAST_KNOWN_LOCATION_LON = 503;
    public static final int SET_LAST_KNOWN_LOCATION_TIME = 504;


    public static final String DEFAULT_FMD_SERVER_URL = "https://fmd.nulide.de";

    public Settings() {
    }

    public <T> void set(int key, T value) {
        super.put(key, value);
        IO.write(JSONFactory.convertSettings(this), IO.settingsFileName);
    }

    public void saveToFile() {
        IO.write(JSONFactory.convertSettings(this), IO.settingsFileName);
    }

    public Object get(int key) {
        if (super.containsKey(key)) {
            return super.get(key);
        } else {
            switch (key) {
                case SET_WIPE_ENABLED:
                case SET_ACCESS_VIA_PIN:
                case SET_FIRST_TIME_WHITELIST:
                case SET_FIRST_TIME_CONTACT_ADDED:
                    //case SET_FIRST_TIME_FMD_SERVER:
                case SET_FMDSERVER_PASSWORD_SET:
                    //case SET_FMD_CRYPT_NEW_SALT:
                case SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED:
                    return false;
                case SET_FMD_LOW_BAT_SEND:
                    return true;
                case SET_FMD_COMMAND:
                    return "fmd";
                case SET_FMDSERVER_UPDATE_TIME:
                    return 60;
                //case SET_INTRODUCTION_VERSION:
                case SET_FMDSMS_COUNTER:
                case SET_FMDSERVER_LOCATION_TYPE:
                case SET_SET_VERSION:
                    return 0;
                case SET_RINGER_TONE:
                    return RingerUtils.getDefaultRingtoneAsString();
                case SET_PIN:
                case SET_FMDSERVER_ID:
                case SET_LAST_KNOWN_LOCATION_LAT:
                case SET_LAST_KNOWN_LOCATION_LON:
                case SET_FMD_CRYPT_HPW:
                case SET_FMD_CRYPT_PRIVKEY:
                case SET_FMD_CRYPT_PUBKEY:
                case SET_FMDSERVER_URL:
                    return "";
                //case SET_GPS_STATE:
                //    return 1;
                case SET_APP_CRASHED_LOG_ENTRY:
                case SET_LAST_KNOWN_LOCATION_TIME:
                    return -1;
            }
        }
        return "";
    }

    public boolean isEmpty(int key) {
        return ((String) get(key)).equals("");
    }

    public FmdKeyPair getKeys() {
        if (get(SET_FMD_CRYPT_PUBKEY).equals("")) {
            return null;
        } else {

            EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(CypherUtils.decodeBase64((String) get(SET_FMD_CRYPT_PUBKEY)));
            PublicKey publicKey = null;
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKey = keyFactory.generatePublic(pubKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }

            return new FmdKeyPair(publicKey, (String) get(SET_FMD_CRYPT_PRIVKEY));
        }
    }

    public void setKeys(FmdKeyPair keys) {
        set(SET_FMD_CRYPT_PRIVKEY, keys.getEncryptedPrivateKey());
        set(SET_FMD_CRYPT_PUBKEY, CypherUtils.encodeBase64(keys.getPublicKey().getEncoded()));
    }

    public void updateSettings() {
        if (((Integer) get(SET_SET_VERSION)) < settingsVersion) {
            if (!((String) get(SET_FMDSERVER_ID)).isEmpty()) {
                FmdKeyPair keys = OldKeyIO.readKeys();
                String HashedPW = OldKeyIO.readHashedPW();
                setKeys(keys);
                set(SET_FMD_CRYPT_HPW, HashedPW);
                set(SET_SET_VERSION, settingsVersion);
            } else {
                set(SET_SET_VERSION, settingsVersion);
            }
        }
    }

    public boolean checkAccountExists() {
        return !((String) get(Settings.SET_FMDSERVER_ID)).isEmpty();
    }

    public static void writeToUri(Context context, Uri uri) {
        try {
            ParcelFileDescriptor sco = context.getContentResolver().openFileDescriptor(uri, "w");
            PrintWriter out = new PrintWriter(new FileOutputStream(sco.getFileDescriptor()));
            Settings settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(settings);
            out.write(json);
            out.close();
            sco.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(context, R.string.settings_exported, Toast.LENGTH_LONG).show();
    }
}
