package de.nulide.findmydevice.data

import android.content.Context
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.utils.SingletonHolder


data class SettingsRepoSpec(val context: Context)


/**
 * Settings should be accessed through this repository.
 * This is to only have a single Settings instance,
 * thus preventing race conditions.
 */
class SettingsRepository private constructor(spec: SettingsRepoSpec) {

    companion object :
        SingletonHolder<SettingsRepository, SettingsRepoSpec>(::SettingsRepository) {

        val TAG = SettingsRepository::class.simpleName
    }

    val settings: Settings

    init {
        IO.context = spec.context
        settings = JSONFactory.convertJSONSettings(
            IO.read(JSONMap::class.java, IO.settingsFileName)
        )
    }
}