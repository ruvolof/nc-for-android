package com.werebug.androidnetcat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

class NetcatSession : AppCompatActivity(), View.OnClickListener {

    private val LogTag: String = "NetcatSessionActivity"

    private lateinit var netcat_session_args: AndroidNetcatHome.sessionArgs
    private lateinit var netcat_cmd_text: String
    private lateinit var tv_netcat_connection: TextView
    private lateinit var et_nc_send_text: EditText
    private lateinit var btn_send: ImageButton

    var myService: NetcatService? = null
    var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netcat_session)

        netcat_session_args = intent.getSerializableExtra(AndroidNetcatHome.netcat_cmd_extra) as AndroidNetcatHome.sessionArgs
        netcat_cmd_text = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string)

        setTitle(netcat_cmd_text)

        tv_netcat_connection = findViewById(R.id.tv_connection)
        et_nc_send_text = findViewById(R.id.et_nc_send_text)

        btn_send = findViewById(R.id.btn_send_text)
        btn_send.setOnClickListener(this)

        val startServiceIntent = Intent(this, NetcatService::class.java)
        bindService(startServiceIntent, myConnection, Context.BIND_AUTO_CREATE)
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NetcatService.MyLocalBinder
            myService = binder.getService()
            isBound = true
            if (netcat_session_args.host != null && netcat_session_args.proto == AndroidNetcatHome.Proto.TCP && !netcat_session_args.listen) {
                myService?.beginTCPConnection(netcat_session_args.host as String, netcat_session_args.port, tv_netcat_connection)
            }
            else if (netcat_session_args.host == null && netcat_session_args.listen && netcat_session_args.proto == AndroidNetcatHome.Proto.TCP) {
                myService?.beginTCPListener(netcat_session_args.port, tv_netcat_connection)
            }
            else if (netcat_session_args.host != null && netcat_session_args.proto == AndroidNetcatHome.Proto.UDP && !netcat_session_args.listen) {
                myService?.beginUDPConnection(netcat_session_args.host as String, netcat_session_args.port, tv_netcat_connection)
            }
            else if (netcat_session_args.host == null && netcat_session_args.listen && netcat_session_args.proto == AndroidNetcatHome.Proto.UDP) {
                myService?.beginUDPListener(netcat_session_args.port, tv_netcat_connection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_send_text -> {
                if (isBound && myService != null) {
                    val text : String = et_nc_send_text.text.toString()
                    et_nc_send_text.text.clear()
                    myService?.send(text.toString())
                }
            }
        }
    }
}
