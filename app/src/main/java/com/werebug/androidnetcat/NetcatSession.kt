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
    private lateinit var worker: NetcatWorker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        netcatSessionArgs =
            intent.getSerializableExtra(AndroidNetcatHome.netcat_cmd_extra)
                    as AndroidNetcatHome.SessionArgs
        title = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string).toString()
        binding.btnSendText.setOnClickListener(this);

        worker = NetcatWorker(netcatSessionArgs, binding.tvConnection)
        worker.start()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_send_text -> {
                val text: String = binding.etNcSendText.text.toString();
                binding.etNcSendText.text.clear()
                worker.addToSendQueue(text)
            }
        }
    }
}
