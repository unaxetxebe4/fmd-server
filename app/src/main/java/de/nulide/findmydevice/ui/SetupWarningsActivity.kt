package de.nulide.findmydevice.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.nulide.findmydevice.databinding.ActivitySetupWarningsBinding
import de.nulide.findmydevice.permissions.globalAppPermissions


class SetupWarningsActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivitySetupWarningsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySetupWarningsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupPermissionsList(
            this,
            viewBinding.permissionsRequiredTitle,
            viewBinding.permissionsRequiredList,
            globalAppPermissions()
        )
    }
}
