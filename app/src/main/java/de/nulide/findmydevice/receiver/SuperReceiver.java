package de.nulide.findmydevice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;

import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.utils.Logger;


abstract class SuperReceiver extends BroadcastReceiver {

    protected Settings settings;

    protected void init(Context context) {
        IO.context = context;
        Logger.init(Thread.currentThread(), context);
        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(context)).getSettings();
    }
}
