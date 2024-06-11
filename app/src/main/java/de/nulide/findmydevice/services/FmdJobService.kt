package de.nulide.findmydevice.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * This alternative JobService base class exists in order to share the
 * coroutineScope for Commands to use to run their asynchronous work in, and
 * to provide a central way to cancel it when the job finishes or is stopped.
 */
abstract class FmdJobService : JobService() {
    companion object {
        private val TAG = this::class.simpleName
    }

    private val coroutineJob = Job()
    val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    private var params: JobParameters? = null

    @CallSuper
    override fun onStartJob(params: JobParameters?): Boolean {
        this.params = params
        return false
    }

    fun jobFinished() {
        Log.d(TAG, "Finishing job ${params?.jobId}")
        coroutineScope.cancel()
        params?.let { this.jobFinished(it, false) }
    }

    @CallSuper
    override fun onStopJob(params: JobParameters?): Boolean {
        coroutineScope.cancel()
        return false
    }
}