package com.werebug.androidnetcat

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.werebug.androidnetcat.databinding.ActivityNetcatSessionBinding
import kotlinx.coroutines.launch

class NetcatSession : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityNetcatSessionBinding
    private val viewModel: NetcatSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val ncCmd = intent.getStringExtra(AndroidNetcatHome.netcat_cmd_string).toString().trim()
        title = ncCmd
        val ncCmdArgv = ncCmd.split(" ").toMutableList()
        val ncatPath = applicationInfo.nativeLibraryDir + "/libncat.so"
        if (ncCmdArgv[0] != "nc" && ncCmdArgv[0] != "ncat") {
            showErrorToast(R.string.error_missing_nc)
            finish()
        }
        ncCmdArgv.removeAt(0)
        ncCmdArgv.add(0, ncatPath)
        // `exec` makes the shell replace itself with ncat, so the process we hold is ncat
        // itself and destroying it on teardown actually stops the listener. It does not help
        // when the command contains a pipe, `;`, `&&` or a redirection: the shell forks for
        // those and the ncat child still outlives the session.
        val shellWrappedArgv =
            listOf("/system/bin/sh", "-c", "exec ${ncCmdArgv.joinToString(" ")}")
        viewModel.startIfNotRunning(shellWrappedArgv)

        binding.btnSendText.setOnClickListener(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { render(it) }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_send_text -> {
                val text: String = binding.etNcSendText.text.toString()
                binding.etNcSendText.text.clear()
                viewModel.send(text)
            }
        }
    }

    private fun showErrorToast(text: Int) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun render(sessionState: NetcatSessionViewModel.SessionState) {
        binding.tvConnection.text = sessionState.output
        val messageViewsVisibility = if (sessionState.running) View.VISIBLE else View.GONE
        binding.etNcSendText.visibility = messageViewsVisibility
        binding.btnSendText.visibility = messageViewsVisibility
    }
}
