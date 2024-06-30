package de.nulide.findmydevice.ui.home

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import de.nulide.findmydevice.permissions.Permission


fun setupPermissionsList(
    activity: ComponentActivity,
    title: TextView,
    list: LinearLayout,
    perms: List<Permission>
) {
    if (perms.isEmpty()) {
        title.visibility = View.GONE
        list.visibility = View.GONE
        return
    }
    title.visibility = View.VISIBLE
    list.visibility = View.VISIBLE

    list.removeAllViews()
    for (p in perms) {
        val pView = PermissionView(title.context)
        pView.setPermission(p, activity)
        list.addView(pView)
        activity.lifecycle.addObserver(pView)
    }
}
