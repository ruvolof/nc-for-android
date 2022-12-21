package com.werebug.androidnetcat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.werebug.androidnetcat.databinding.ActivityNetcatSessionBinding

class NetcatSession : AppCompatActivity(), View.OnClickListener {

    private val LogTag: String = "NetcatSessionActivity"

    private lateinit var binding: ActivityNetcatSessionBinding;
    private lateinit var netcatSessionArgs: AndroidNetcatHome.SessionArgs

    var myService: NetcatService? = null
    var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        netcatSessionArgs =
            intent.getSerializableExtra(AndroidNetcatHome.netcat_cmd_extra)
                    as AndroidNetcatHome.SessionArgs
        title = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string).toString()
        binding.btnSendText.setOnClickListener(this);
        val startServiceIntent = Intent(this, NetcatService::class.java)
        bindService(startServiceIntent, myConnection, Context.BIND_AUTO_CREATE)
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as NetcatService.MyLocalBinder
            myService = binder.getService()
            isBound = true
            myService?.beginConnection(netcatSessionArgs, binding.tvConnection)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_send_text -> {
                if (isBound && myService != null) {
                    val text: String = binding.etNcSendText.text.toString();
                    binding.etNcSendText.text.clear()
                    myService?.send(text)
                }
            }
        }
    }
}
