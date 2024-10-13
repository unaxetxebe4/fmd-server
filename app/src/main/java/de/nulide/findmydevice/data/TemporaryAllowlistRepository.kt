package de.nulide.findmydevice.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import de.nulide.findmydevice.utils.SingletonHolder
import java.io.File
import java.io.FileReader
import java.util.LinkedList


const val TEMP_ALLOWLIST_FILENAME = "temporary_allowlist.json"
const val TEMP_USAGE_VALIDITY_MILLIS = 10 * 60 * 1000 // 10 min


data class TempAllowedNumber(
    val number: String,
    val subscriptionId: Int,
    val createdTimeMillis: Long,
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return createdTimeMillis + TEMP_USAGE_VALIDITY_MILLIS < now
    }
}

class TemporaryAllowlistModel : LinkedList<TempAllowedNumber>()


class TemporaryAllowlistRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<TemporaryAllowlistRepository, Context>(::TemporaryAllowlistRepository) {}

    private val gson = Gson()

    private val list: TemporaryAllowlistModel

    init {
        val file = File(context.filesDir, TEMP_ALLOWLIST_FILENAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        val reader = JsonReader(FileReader(file))
        list = gson.fromJson(reader, TemporaryAllowlistModel::class.java)
            ?: TemporaryAllowlistModel()
    }

    private fun saveList() {
        val raw = gson.toJson(list)
        val file = File(context.filesDir, TEMP_ALLOWLIST_FILENAME)
        file.writeText(raw)
    }

    fun containsValidNumber(number: String): Boolean {
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                if (ele.isExpired()) {
                    list.remove(ele)
                    saveList()
                } else {
                    return true
                }
            }
        }
        return false
    }

    fun add(number: String, subscriptionId: Int) {
        val now = System.currentTimeMillis()

        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                val new = ele.copy(createdTimeMillis = now)
                list.remove(ele)
                list.add(new)
                saveList()
                return
            }
        }

        // If it was not in the list
        list.add(TempAllowedNumber(number, subscriptionId, now))
        saveList()
    }

    /**
     * Removes all entries that have expired from the temporary allowlist.
     */
    fun removeExpired(): List<Pair<String, Int>> {
        val expired = mutableListOf<Pair<String,Int>>()
        val toRemove = mutableListOf<TempAllowedNumber>()
        for (ele in list) {
            if (ele.isExpired()) {
                toRemove.add(ele)
                expired.add(ele.number to ele.subscriptionId)
            }
        }
        for (ele in toRemove){
            list.remove(ele)
        }
        saveList()
        return expired
    }
}
