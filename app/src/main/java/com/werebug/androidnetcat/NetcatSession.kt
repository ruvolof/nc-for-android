package com.werebug.androidnetcat

import android.os.Bundle
import android.view.MenuItem
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val ncCmd = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string).toString().trim()
        title = ncCmd
        val ncatPath = applicationInfo.nativeLibraryDir + "/libncat.so"
        
        if (!ncCmd.startsWith("nc ") && !ncCmd.startsWith("ncat ") && ncCmd != "nc" && ncCmd != "ncat") {
            showErrorToast(R.string.error_missing_nc)
            finish()
            return
        }
        
        val ncatCommand = if (ncCmd.startsWith("nc ")) {
            ncCmd.replaceFirst("nc ", "$ncatPath ")
        } else if (ncCmd.startsWith("ncat ")) {
            ncCmd.replaceFirst("ncat ", "$ncatPath ")
        } else if (ncCmd == "nc" || ncCmd == "ncat") {
            ncatPath
        } else {
            ncatPath // Fallback
        }

        val shellWrappedArgv = listOf("/system/bin/sh", "-c", ncatCommand)
        worker = NetcatWorker(shellWrappedArgv, WeakReference(this))
        worker.start()

        binding.btnSendText.setOnClickListener(this);
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_session, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_clear -> {
                binding.tvConnection.text = ""
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        binding.svConnection.post {
            binding.svConnection.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun disableMessageViews() {
        binding.etNcSendText.visibility = View.GONE
        binding.btnSendText.visibility = View.GONE
    }
}
