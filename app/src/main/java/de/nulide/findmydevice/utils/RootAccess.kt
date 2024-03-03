package de.nulide.findmydevice.utils

import android.R.attr.process
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.StringBuilder


class RootAccess {
    companion object {
        @JvmStatic
        fun isRooted(): Boolean {
            var proc: Process? = null
            return try {
                proc = Runtime.getRuntime().exec("su -c exit")
                proc.waitFor() == 0
            } catch (e: Exception) {
                false
            } finally {
                proc?.destroy()
            }
        }

        @JvmStatic
        fun execCommand(com: String) {
            var proc: Process? = null
            try {
                val toExec = "$com && echo hi && exit\n"
                proc = Runtime.getRuntime().exec("su")

                val outputStream: OutputStream = proc.outputStream
                outputStream.write(toExec.toByteArray())
                outputStream.flush()
                outputStream.close()

                proc.waitFor()


            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                proc?.destroy()
            }
        }
    }
}
