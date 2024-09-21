package de.nulide.findmydevice.ui.settings

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.LogRepository


class LogViewActivity : AppCompatActivity() {

    private lateinit var repo: LogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        repo = LogRepository.getInstance(this)

        // TODO: Observe list as LiveData or Flow
        val adapter = LogViewAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_logs)
        recyclerView.adapter = adapter

        adapter.submitList(repo.list)
        recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_log_view, menu)
        return true
    }
}
