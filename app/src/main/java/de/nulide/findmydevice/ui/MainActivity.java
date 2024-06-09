package de.nulide.findmydevice.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.ui.home.CommandListFragment;
import de.nulide.findmydevice.ui.home.SettingsFragment;
import de.nulide.findmydevice.ui.home.TransportListFragment;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.ui.settings.SettingsActivity;
import de.nulide.findmydevice.utils.Logger;
import de.nulide.findmydevice.utils.Notifications;
import de.nulide.findmydevice.utils.Permission;


public class MainActivity extends AppCompatActivity {

    private Settings settings;

    private Fragment commandsFragment, transportFragment, settingsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PushReceiver.registerWithUnifiedPush(this);

        IO.context = this;
        Logger.init(Thread.currentThread(), this);
        Notifications.init(this, false);
        Permission.initValues(this);

        settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) != -1) {
            Intent intent = new Intent(this, CrashedActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        settings.updateSettings();
        if (!settings.isIntroductionPassed() || !Permission.CORE) {
            Intent intent = new Intent(this, IntroductionActivity.class);
            startActivity(intent);
            finish();
            return;

        }
        if (!(Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED)) {
            Intent intent = new Intent(this, UpdateboardingModernCryptoActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (settings.checkAccountExists()) {
            FMDServerLocationUploadService.scheduleJob(this, 0);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(navListener);

        commandsFragment = new CommandListFragment();
        transportFragment = new TransportListFragment();
        settingsFragment = new SettingsFragment();
        activeFragment = commandsFragment;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Permission.initValues(this);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment)
                .commit();
    }

    private final NavigationBarView.OnItemSelectedListener navListener = (item) -> {
        switch (item.getItemId()) {
            case R.id.nav_commands: {
                activeFragment = commandsFragment;
                break;
            }
            case R.id.nav_transports: {
                activeFragment = transportFragment;
                break;
            }
            case R.id.nav_settings: {
                activeFragment = settingsFragment;
                break;
            }
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment)
                .commit();
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSettings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
