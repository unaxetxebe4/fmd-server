package de.nulide.findmydevice.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.SettingsRepoSpec;
import de.nulide.findmydevice.data.SettingsRepository;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.permissions.PermissionsUtilKt;
import de.nulide.findmydevice.receiver.PushReceiver;
import de.nulide.findmydevice.services.FMDServerLocationUploadService;
import de.nulide.findmydevice.services.TempContactExpiredService;
import de.nulide.findmydevice.ui.home.CommandListFragment;
import de.nulide.findmydevice.ui.home.TransportListFragment;
import de.nulide.findmydevice.ui.onboarding.UpdateboardingModernCryptoActivity;
import de.nulide.findmydevice.ui.settings.SettingsFragment;


public class MainActivity extends AppCompatActivity {

    private static final String KEY_ACTIVE_FRAGMENT_TAG = "activeFragmentTag";

    private TaggedFragment commandsFragment, transportFragment, settingsFragment;
    private TaggedFragment activeFragment;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // for some reason, getTag() returns null, so we need to use getStaticTag()
        outState.putString(KEY_ACTIVE_FRAGMENT_TAG, activeFragment.getStaticTag());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IO.context = this;

        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) == 1) {
            Intent intent = new Intent(this, CrashedActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        settings.updateSettings();
        if (!(Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED)) {
            Intent intent = new Intent(this, UpdateboardingModernCryptoActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(navListener);

        commandsFragment = new CommandListFragment();
        transportFragment = new TransportListFragment();
        settingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            activeFragment = commandsFragment;
        } else {
            String tag = savedInstanceState.getString(KEY_ACTIVE_FRAGMENT_TAG);
            if (tag == null || tag.equals(commandsFragment.getStaticTag())) {
                activeFragment = commandsFragment;
            } else if (tag.equals(transportFragment.getStaticTag())) {
                activeFragment = transportFragment;
            } else if (tag.equals(settingsFragment.getStaticTag())) {
                activeFragment = settingsFragment;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment, activeFragment.getStaticTag())
                .commit();

        Settings settings = SettingsRepository.Companion.getInstance(new SettingsRepoSpec(this)).getSettings();
        if (settings.checkAccountExists()) {
            PushReceiver.registerWithUnifiedPush(this);
            FMDServerLocationUploadService.scheduleJob(this, 0);
        } else {
            // just in case it was still running
            FMDServerLocationUploadService.cancelJob(this);
        }
        TempContactExpiredService.scheduleJob(this, 0);
        invalidateOptionsMenu();
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
        if (PermissionsUtilKt.isMissingGlobalAppPermission(this)) {
            getMenuInflater().inflate(R.menu.main_app_bar_warnings, menu);
        } else {
            getMenuInflater().inflate(R.menu.main_app_bar, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSetupWarnings) {
            Intent intent = new Intent(this, SetupWarningsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
