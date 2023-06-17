package com.werebug.androidnetcat

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.werebug.androidnetcat.databinding.ActivityNetcatSessionBinding

class NetcatSession : AppCompatActivity(), View.OnClickListener {

    private val LogTag: String = "NetcatSessionActivity"

    private lateinit var binding: ActivityNetcatSessionBinding;
    private lateinit var netcatSessionArgs: AndroidNetcatHome.SessionArgs
    private lateinit var worker: NetcatWorker

    @Suppress("DEPRECATION")
    private fun getNetcatSessionArgs(): AndroidNetcatHome.SessionArgs {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                AndroidNetcatHome.netcat_cmd_extra,
                AndroidNetcatHome.SessionArgs::class.java
            )!!
        } else {
            intent.getSerializableExtra(
                AndroidNetcatHome.netcat_cmd_extra
            ) as AndroidNetcatHome.SessionArgs
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        netcatSessionArgs = getNetcatSessionArgs();
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
