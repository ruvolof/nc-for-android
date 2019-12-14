package com.werebug.androidnetcat

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import java.util.*

class NetcatService : Service() {

    private val LogTag: String = "NetcatService"

    private val myBinder = MyLocalBinder()

    private val sendQueue : LinkedList<String> = LinkedList()

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : NetcatService {
            return this@NetcatService
        }
    }

    fun beginNetcatConnection(nCmd: String, tView: TextView) {
        Log.d(LogTag, "Started with: " + nCmd)
        val params: List<String> = nCmd.split(" ")
        val host: String = params[1]
        val port: Int = params[2].toInt()

        val worker = NetcatWorker(host, port, tView, sendQueue)
        worker.start()
    }

    fun send(text: String) {
        sendQueue.add(text)
    }

    fun beginNetcatListener(nCmd: String, tView: TextView) {
        val params: List<String> = nCmd.split(" ")
        val port: Int = params[2].toInt()

        val worker = NetcatWorker(null, port, tView, sendQueue)
        worker.start()
    }
}
