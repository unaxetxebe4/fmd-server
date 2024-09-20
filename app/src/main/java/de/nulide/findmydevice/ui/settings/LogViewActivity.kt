package de.nulide.findmydevice.ui.settings

import android.os.Bundle
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.LogRepository
import java.util.Date


class LogViewActivity : AppCompatActivity() {

    private lateinit var repo: LogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        repo = LogRepository.getInstance(this)

        val items = repo.list.map {
            val date = Date(it.time)

            it.level + " " + date.toString() + " - " + it.tag + "\n" + it.msg
        }.reversed()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

        val listView = findViewById<ListView>(R.id.listLog)
        listView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_log_view, menu)
        return true
    }
}
