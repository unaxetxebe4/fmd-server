package de.nulide.findmydevice.ui.helper

import kotlin.reflect.KClass


data class ConfigurationActivityInformation(
    val nameResourceId: Int,
    val activityClass: KClass<*>,
)
