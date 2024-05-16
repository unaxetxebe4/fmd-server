package de.nulide.findmydevice.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.nulide.findmydevice.R

class Utils {

    companion object {
        @JvmStatic
        fun copyToClipboard(context: Context, label: String?, text: String?) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                val copied = context.getString(R.string.copied)
                Toast.makeText(context, copied, Toast.LENGTH_LONG).show()
            }
        }

        @JvmStatic
        fun pasteFromClipboard(context: Context): CharSequence {
            val clipboardManager =
                context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboardManager.primaryClip!!.getItemAt(0)
            return item.text
        }

        @JvmStatic
        fun openUrl(context: Context, url: String?) {
            val uri = Uri.parse(url)
            val i = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(i)
        }

        @JvmStatic
        fun getGeoURI(lat: String, lon: String): String {
            return "geo:$lat,$lon"
        }

        @JvmStatic
        fun getOpenStreetMapLink(lat: String, lon: String): String {
            return "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon&zoom=14"
        }
    }
}