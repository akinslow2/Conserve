package com.gemini.energy.service.sync

import android.content.Context
import com.gemini.energy.App
import com.gemini.energy.service.ParseAPI

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

        val mColListenerDownload: Collection.Listener = object : Collection.Listener {
            override fun onPreExecute() {
            }

            override fun onPostExecute(col: Collection?) {
                col?.let {
                    val syncer = Syncer(parseAPIService, it, mSyncListener)
                    syncer.gDownload()
                }
            }
        }

        Collection.create(mColListenerDownload)
    }
}
