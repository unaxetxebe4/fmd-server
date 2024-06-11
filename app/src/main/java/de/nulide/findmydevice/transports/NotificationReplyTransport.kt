package de.nulide.findmydevice.transports

import android.app.PendingIntent.CanceledException
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.robj.notificationhelperlibrary.utils.NotificationUtils
import de.nulide.findmydevice.permissions.NotificationAccessPermission


class NotificationReplyTransport(
    private val destination: StatusBarNotification
) : Transport<StatusBarNotification>(destination) {
    companion object {
        private val TAG = this::class.simpleName
    }

    override val requiredPermissions = listOf(NotificationAccessPermission())

    override fun send(context: Context, msg: String) {
        super.send(context, msg)

        val action = NotificationUtils.getQuickReplyAction(
            destination.notification, context.packageName
        )
        if (action == null) {
            Log.i(TAG, "Cannot send message: quick reply action was null")
            return
        }
        try {
            action.sendReply(context, msg)
        } catch (e: CanceledException) {
            Log.e(TAG, "Failed to send message via notification reply")
            e.printStackTrace()
        }
    }
}
