package de.nulide.findmydevice.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.WhiteList;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.data.io.json.JSONWhiteList;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.ui.settings.SettingsActivity;
import de.nulide.findmydevice.ui.settings.WhiteListActivity;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.Permission;
import host.stjin.expandablecardview.ExpandableCardView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewFMDCommandName;
    private TextView textViewWhiteListCount;
    private TextView textViewCORE;
    private TextView textViewGPS;
    private TextView textViewDND;
    private TextView textViewDeviceAdmin;
    private TextView textViewWriteSecureSettings;
    private TextView textViewOverlay;
    private TextView textViewNotification;
    private TextView textViewCamera;
    private TextView textViewBatteryOptimization;
    private TextView textViewServerServiceEnabled;
    private TextView textViewServerRegistered;
    private TextView textViewPush;
    private Button buttonOpenWhitelist;
    private ExpandableCardView expandableCardViewPermissions;

    private WhiteList whiteList;
    private Settings Settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PushReceiver.registerWithUnifiedPush(this);
        IO.context = this;
        Logger.init(Thread.currentThread(), this);
        Notifications.init(this, false);
        whiteList = JSONFactory.convertJSONWhiteList(IO.read(JSONWhiteList.class, IO.whiteListFileName));
        Settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        Permission.initValues(this);
        if(((Integer) Settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) != -1){
            Intent crash = new Intent(this, CrashedActivity.class);
            startActivity(crash);
        }
        Settings.updateSettings();
        // Show the IntroductionActivity again if one of the important permissions is missing
        if (!Settings.isIntroductionPassed() || !Permission.CORE) {
            Intent introductionIntent = new Intent(this, IntroductionActivity.class);
            startActivity(introductionIntent);
        }
        if(Settings.checkAccountExists()) {
            FMDServerService.checkForOldSalt(this);
            FMDServerService.scheduleJob(this, 0);
        }
        reloadViews();
        updateViews();
    }

    public void reloadViews() {
        textViewFMDCommandName = findViewById(R.id.textViewFMDCommandName);
        textViewWhiteListCount = findViewById(R.id.textViewWhiteListCount);
        textViewCORE = findViewById(R.id.textViewCORE);
        textViewGPS = findViewById(R.id.textViewGPS);
        textViewDND = findViewById(R.id.textViewDND);
        textViewDeviceAdmin = findViewById(R.id.textViewDeviceAdmin);
        textViewWriteSecureSettings = findViewById(R.id.textViewWriteSecureSettings);
        textViewOverlay = findViewById(R.id.textViewOverlay);
        textViewNotification = findViewById(R.id.textViewNotification);
        buttonOpenWhitelist = findViewById(R.id.buttonOpenWhiteList);
        buttonOpenWhitelist.setOnClickListener(this);
        textViewServerServiceEnabled = findViewById(R.id.textViewServerEnabled);
        textViewServerRegistered = findViewById(R.id.textViewRegisteredOnServer);
        textViewPush = findViewById(R.id.textViewPushAvailable);
        textViewCamera = findViewById(R.id.textViewCamera);
        textViewBatteryOptimization = findViewById(R.id.textviewBatteryOptimization);
        expandableCardViewPermissions = findViewById(R.id.expandableCardViewPermissions);
    }

    public void updateViews() {
        textViewFMDCommandName.setText((String) Settings.get(Settings.SET_FMD_COMMAND));

        int colorEnabled;
        int colorDisabled;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            colorEnabled = getColor(R.color.colorEnabled);
            colorDisabled = getColor(R.color.colorDisabled);
        } else {
            colorEnabled = getResources().getColor(R.color.colorEnabled);
            colorDisabled = getResources().getColor(R.color.colorDisabled);
        }

        ForegroundColorSpan whitelistCountColor;
        if (whiteList.size() > 0) {
            whitelistCountColor = new ForegroundColorSpan(colorEnabled);
        } else {
            whitelistCountColor = new ForegroundColorSpan(colorDisabled);
        }
        String whitelistPrefix = getString(R.string.Settings_WhiteList) + ": ";
        String whitelistAll = whitelistPrefix + whiteList.size();
        Spannable spannable = new SpannableString(whitelistAll);
        spannable.setSpan(whitelistCountColor, whitelistPrefix.length(), whitelistAll.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewWhiteListCount.setText(spannable, TextView.BufferType.SPANNABLE);

        if (Permission.CORE) {
            textViewCORE.setText(getString(R.string.Enabled));
            textViewCORE.setTextColor(colorEnabled);
        } else {
            textViewCORE.setText(getString(R.string.Disabled));
            textViewCORE.setTextColor(colorDisabled);
        }
        if (Permission.GPS) {
            textViewGPS.setText(getString(R.string.Enabled));
            textViewGPS.setTextColor(colorEnabled);
        } else {
            textViewGPS.setText(getString(R.string.Disabled));
            textViewGPS.setTextColor(colorDisabled);
        }
        if (Permission.DND) {
            textViewDND.setText(getString(R.string.Enabled));
            textViewDND.setTextColor(colorEnabled);
        } else {
            textViewDND.setText(getString(R.string.Disabled));
            textViewDND.setTextColor(colorDisabled);
        }
        if (Permission.DEVICE_ADMIN) {
            textViewDeviceAdmin.setText(getString(R.string.Enabled));
            textViewDeviceAdmin.setTextColor(colorEnabled);
        } else {
            textViewDeviceAdmin.setText(getString(R.string.Disabled));
            textViewDeviceAdmin.setTextColor(colorDisabled);
        }
        if (Permission.WRITE_SECURE_SETTINGS) {
            textViewWriteSecureSettings.setText(getString(R.string.Enabled));
            textViewWriteSecureSettings.setTextColor(colorEnabled);
        } else {
            textViewWriteSecureSettings.setText(getString(R.string.Disabled));
            textViewWriteSecureSettings.setTextColor(colorDisabled);
        }
        if (Permission.OVERLAY) {
            textViewOverlay.setText(getString(R.string.Enabled));
            textViewOverlay.setTextColor(colorEnabled);
        } else {
            textViewOverlay.setText(getString(R.string.Disabled));
            textViewOverlay.setTextColor(colorDisabled);
        }
        if (Permission.NOTIFICATION_ACCESS) {
            textViewNotification.setText(getString(R.string.Enabled));
            textViewNotification.setTextColor(colorEnabled);
        } else {
            textViewNotification.setText(getString(R.string.Disabled));
            textViewNotification.setTextColor(colorDisabled);
        }
        if(Permission.CAMERA){
            textViewCamera.setText(getString(R.string.Enabled));
            textViewCamera.setTextColor(colorEnabled);
        } else {
            textViewCamera.setText(getString(R.string.Disabled));
            textViewCamera.setTextColor(colorDisabled);
        }
        if(Permission.BATTERY_OPTIMIZATION){
            textViewBatteryOptimization.setText(R.string.Enabled);
            textViewBatteryOptimization.setTextColor(colorEnabled);
        } else {
            textViewBatteryOptimization.setText(R.string.Disabled);
            textViewBatteryOptimization.setTextColor(colorDisabled);
        }
        expandableCardViewPermissions.setTitle(-1, getString(R.string.Granted) + " " + Permission.ENABLED_PERMISSIONS + "/" + Permission.AVAILABLE_PERMISSIONS);

        if(Settings.checkAccountExists()){
            textViewServerServiceEnabled.setText(getString(R.string.Enabled));
            textViewServerServiceEnabled.setTextColor(colorEnabled);
        }else{
            textViewServerServiceEnabled.setText(getString(R.string.Disabled));
            textViewServerServiceEnabled.setTextColor(colorDisabled);
        }

        if(Settings.checkAccountExists()){
            textViewServerRegistered.setText(getString(R.string.Enabled));
            textViewServerRegistered.setTextColor(colorEnabled);
        }else{
            textViewServerRegistered.setText(getString(R.string.Disabled));
            textViewServerRegistered.setTextColor(colorDisabled);
        }

        if (PushReceiver.isRegisteredWithUnifiedPush(this)) {
            textViewPush.setText(R.string.Available);
            textViewPush.setTextColor(colorEnabled);
        } else {
            textViewPush.setTextColor(colorDisabled);
            textViewPush.setText(R.string.NOT_AVAILABLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menuItemSettings){
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Permission.initValues(this);
        whiteList = JSONFactory.convertJSONWhiteList(IO.read(JSONWhiteList.class, IO.whiteListFileName));
        Settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        if (!Settings.isIntroductionPassed() || !Permission.CORE) {
            Intent introductionIntent = new Intent(this, IntroductionActivity.class);
            startActivity(introductionIntent);
        }
        updateViews();
    }

    @Override
    public void onClick(View v) {
        if(v == buttonOpenWhitelist){
            Intent whiteListActivity = new Intent(this, WhiteListActivity.class);
            startActivity(whiteListActivity);
        }
    }
}
