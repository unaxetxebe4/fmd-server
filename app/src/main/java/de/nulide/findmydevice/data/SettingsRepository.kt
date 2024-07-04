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

    var settings: Settings
        private set
        get() {
            // FIXME: there are still race conditions between Activities/Fragments/Jobs :/
            // So for now, force reload the file on every get.
            forceReload()
            return field
        }

    init {
        IO.context = spec.context
        settings = JSONFactory.convertJSONSettings(
            IO.read(JSONMap::class.java, IO.settingsFileName)
        )
    }

    /**
     * Reload the settings.
     * Call this when you know the underlying file has changed,
     * e.g., after importing the settings.
     */
    fun forceReload() {
        settings = JSONFactory.convertJSONSettings(
            IO.read(JSONMap::class.java, IO.settingsFileName)
        )
    }
}