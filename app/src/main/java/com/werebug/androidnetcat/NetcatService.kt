package com.werebug.androidnetcat

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.TextView
import java.util.*

class NetcatService : Service() {

    private val myBinder = MyLocalBinder()
    private val sendQueue: LinkedList<String> = LinkedList()

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): NetcatService {
            return this@NetcatService
        }
    }

    fun send(text: String) {
        sendQueue.add(text)
    }

    fun beginConnection(sessionArgs: AndroidNetcatHome.SessionArgs, tView: TextView) {
        val worker = NetcatWorker(sessionArgs, tView, sendQueue)
        worker.start()
    }
}
