package de.nulide.findmydevice.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import de.nulide.findmydevice.R
import java.io.FileNotFoundException
import java.io.OutputStream
import java.io.OutputStreamWriter


fun writeToUri(
    context: Context,
    uri: Uri,
    src: Any,
) {
    val outputStream: OutputStream
    try {
        outputStream = context.contentResolver.openOutputStream(uri) ?: return
    } catch (e: FileNotFoundException) {
        context.log().e("writeToUri", "Export failed:\n${e.stackTraceToString()}")
        Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
        return
    }

    val type = src.javaClass
    val writer = JsonWriter(OutputStreamWriter(outputStream))

    Gson().toJson(src, type, writer)

    writer.close()
    outputStream.close()

    Toast.makeText(context, R.string.export_success, Toast.LENGTH_LONG).show()
}
