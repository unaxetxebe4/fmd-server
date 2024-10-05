package de.nulide.findmydevice.services

import android.content.Context
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import de.nulide.findmydevice.commands.CommandHandler
import de.nulide.findmydevice.commands.CommandHandler.Companion.checkAndRemovePin
import de.nulide.findmydevice.data.Allowlist
import de.nulide.findmydevice.data.ConfigSMSRec
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepoSpec
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.data.io.IO
import de.nulide.findmydevice.data.io.JSONFactory
import de.nulide.findmydevice.data.io.json.JSONMap
import de.nulide.findmydevice.data.io.json.JSONWhiteList
import de.nulide.findmydevice.receiver.BatteryLowReceiver
import de.nulide.findmydevice.transports.NotificationReplyTransport
import de.nulide.findmydevice.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.util.Calendar

class ThirdPartyAccessService : NotificationListenerService() {

    private lateinit var settings: Settings
    private lateinit var allowlist: Allowlist
    private lateinit var config: ConfigSMSRec

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    fun init(context: Context) {
        IO.context = context
        Logger.init(Thread.currentThread(), context)

        allowlist = JSONFactory.convertJSONWhiteList(
            IO.read(
                JSONWhiteList::class.java,
                IO.whiteListFileName
            )
        )
        settings = SettingsRepository.getInstance(SettingsRepoSpec(this)).settings
        config = JSONFactory.convertJSONConfig(IO.read(JSONMap::class.java, IO.SMSReceiverTempData))

        if (config.get(ConfigSMSRec.CONF_LAST_USAGE) == null) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, -5)
            config.set(ConfigSMSRec.CONF_LAST_USAGE, cal.timeInMillis)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        init(this)

        // SMS is handled separately
        val packageName = sbn.packageName
        if (packageName == Telephony.Sms.getDefaultSmsPackage(this)) {
            return
        }

        if (settings[Settings.SET_FMD_LOW_BAT_SEND] as Boolean) {
            if (packageName == "com.android.systemui") {
                val tag = sbn.tag
                if (tag != null && tag == "low_battery") {
                    BatteryLowReceiver.handleLowBatteryUpload(this)
                    return
                }
            }
        }

        val messageChars = sbn.notification.extras.getCharSequence("android.text") ?: return
        var message = messageChars.toString().lowercase()

        val fmdTriggerWord = settings[Settings.SET_FMD_COMMAND] as String
        if (message.contains(fmdTriggerWord)) {
            val newMessage = checkAndRemovePin(settings, message)
            if (newMessage == null) {
                // TODO: wrong PIN!
                return
            }
            message = newMessage

            val transport = NotificationReplyTransport(sbn)
            val commandHandler = CommandHandler(transport, coroutineScope, null)
            commandHandler.execute(this, message)

            cancelNotification(sbn.key)
        }
    }
}
