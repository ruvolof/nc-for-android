package com.werebug.androidnetcat

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
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

    fun send(text: String) {
        sendQueue.add(text)
    }

    fun beginTCPConnection(host: String, port: Int, tView: TextView) {
        val worker = NetcatWorker(host, port, AndroidNetcatHome.Proto.TCP, tView, sendQueue)
        worker.start()
    }

    fun beginTCPListener(port: Int, tView: TextView) {
        val worker = NetcatWorker(null, port, AndroidNetcatHome.Proto.TCP, tView, sendQueue)
        worker.start()
    }

    fun beginUDPConnection(host: String, port: Int, tView: TextView) {
        val worker = NetcatWorker(host, port, AndroidNetcatHome.Proto.UDP, tView, sendQueue)
        worker.start()
    }

    fun beginUDPListener(port: Int, tView: TextView) {
        val worker = NetcatWorker(null, port, AndroidNetcatHome.Proto.UDP, tView, sendQueue)
        worker.start()
    }
}
