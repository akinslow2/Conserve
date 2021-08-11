package com.gemini.energy.service.sync

import android.content.Context
import com.gemini.energy.App
import com.gemini.energy.service.ParseAPI
import timber.log.Timber

class Connection {
    /**
     * Parse API Service Call
     * */
    private val parseAPIService by lazy { ParseAPI.create() }
    private var context: Context? = null

    init {
        this.context = App.instance
    }

    fun sync(mSyncListener: Syncer.Listener? = null) {
        Timber.i("~~~~~ Connection: sync(${mSyncListener})")

        val mColListenerDownload: Collection.Listener = object : Collection.Listener {
            override fun onPreExecute() {
                Timber.i("~~~~~ Connection: Collection.Listener onPreExecute()")
            }

            override fun onPostExecute(col: Collection?) {
                Timber.i("~~~~~ Connection: Collection.Listener onPostExecute($col)")
                col?.let {
                    val syncer = Syncer(parseAPIService, it, mSyncListener)
                    syncer.gDownload()
                }
            }
        }

        Collection.create(mColListenerDownload)
    }
}