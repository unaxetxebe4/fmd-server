package de.nulide.findmydevice.ui.home

import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.ui.setupPermissionsList


class TransportListViewHolder(
    private val activity: ComponentActivity,
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Transport<*>) {
        val context = itemView.context

        itemView.findViewById<TextView>(R.id.title).apply {
            text = context.getString(item.title)
            val drawable = ContextCompat.getDrawable(context, item.icon)
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        }

        itemView.findViewById<TextView>(R.id.description).text = context.getString(item.description)

        itemView.findViewById<TextView>(R.id.description_auth).text =
            context.getString(item.descriptionAuth)

        val noteRes = item.descriptionNote
        if (noteRes == null) {
            itemView.findViewById<View>(R.id.description_note).visibility = View.GONE
        } else {
            itemView.findViewById<TextView>(R.id.description_note).text = context.getString(noteRes)
            itemView.findViewById<View>(R.id.description_note).visibility = View.VISIBLE
        }

        val permReqTitle = itemView.findViewById<TextView>(R.id.permissions_required_title)
        val permReqList = itemView.findViewById<LinearLayout>(R.id.permissions_required_list)
        setupPermissionsList(activity, permReqTitle, permReqList, item.requiredPermissions)

        val configActivityInfo = item.configActivityInfo
        if (configActivityInfo != null) {
            val configurationButton =
                itemView.findViewById<Button>(R.id.button_configure_transport)
            configurationButton.visibility = View.VISIBLE
            configurationButton.text = activity.getString(configActivityInfo.nameRessourceId)
            configurationButton.setOnClickListener {
                val configurationIntent = Intent(context, configActivityInfo.actvityClass.java);
                activity.startActivity(configurationIntent)
            }
        }

    }
}
