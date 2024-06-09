package de.nulide.findmydevice.ui.home

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.Command
import de.nulide.findmydevice.permissions.Permission


class CommandListViewHolder(
    private val activity: ComponentActivity,
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Command) {
        val context = itemView.context

        itemView.findViewById<TextView>(R.id.usage).text = item.usage

        itemView.findViewById<TextView>(R.id.description_short).text =
            context.getString(item.shortDescription)

        val textViewLongDescription = itemView.findViewById<TextView>(R.id.description_long)
        val longDesc = item.longDescription
        if (longDesc != null) {
            textViewLongDescription.text = context.getString(longDesc)
            textViewLongDescription.visibility = View.VISIBLE
        } else {
            textViewLongDescription.visibility = View.GONE
        }

        // Required permissions
        val permReqTitle = itemView.findViewById<TextView>(R.id.permissions_required_title)
        val permReqList = itemView.findViewById<LinearLayout>(R.id.permissions_required_list)
        setupPermissionsList(permReqTitle, permReqList, item.requiredPermissions)

        // Optional permissions
        val permOptTitle = itemView.findViewById<TextView>(R.id.permissions_optional_title)
        val permOptList = itemView.findViewById<LinearLayout>(R.id.permissions_optional_list)
        setupPermissionsList(permOptTitle, permOptList, item.optionalPermissions)
    }

    private fun setupPermissionsList(title: TextView, list: LinearLayout, perms: List<Permission>) {
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
}
