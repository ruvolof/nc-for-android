package com.werebug.androidnetcat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.werebug.androidnetcat.databinding.ActivityNetcatSessionBinding
import java.lang.ref.WeakReference

class NetcatSession : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityNetcatSessionBinding;
    private lateinit var worker: NetcatWorker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ncCmd = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string).toString()
        title = ncCmd
        val ncCmdArgv = ncCmd.split(" ").toMutableList()
        val ncatPath = applicationInfo.nativeLibraryDir + "/libncat.so"
        if (ncCmdArgv[0] != "nc" && ncCmdArgv[0] != "ncat") {
            showErrorToast(R.string.error_missing_nc)
            finish()
        }
        ncCmdArgv.removeAt(0)
        ncCmdArgv.add(0, ncatPath)
        worker = NetcatWorker(ncCmdArgv, WeakReference(this))
        worker.start()

        binding.btnSendText.setOnClickListener(this);
    }

    override fun onDestroy() {
        worker.halt()
        super.onDestroy()
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

    private fun showErrorToast(text: Int) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    fun appendToOutputView(message: String) {
        val newText = "${binding.tvConnection.text}${message}"
        binding.tvConnection.text = newText
    }

    fun disableMessageViews() {
        binding.etNcSendText.visibility = View.GONE
        binding.btnSendText.visibility = View.GONE
    }
}
