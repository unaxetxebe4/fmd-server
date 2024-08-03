package de.nulide.findmydevice.transports

import kotlin.reflect.KClass


data class TransportConfigInfo(
    val nameResourceId: Int,
    val activityClass: KClass<*>,
)
