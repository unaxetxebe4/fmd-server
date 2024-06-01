package de.nulide.findmydevice.ui.allowlist

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R

class AllowlistViewHolder(
    itemView: View,
    private val onDeleteClicked: (String) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: AllowlistItem) {
        itemView.findViewById<TextView>(R.id.text_name).text = item.name
        itemView.findViewById<TextView>(R.id.text_number).text = item.number

        itemView.findViewById<ImageView>(R.id.button_delete)
            .setOnClickListener { onDeleteClicked(item.number) }
    }
}
