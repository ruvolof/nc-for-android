package com.werebug.androidnetcat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.werebug.androidnetcat.databinding.ActivityNetcatHomeBinding

class AndroidNetcatHome : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val netcat_cmd_string: String = "NETCAT_CMD"
    }

    private lateinit var binding: ActivityNetcatHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartNetcat.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_netcat -> {
                startNetcatSessionActivity(binding.etNcCommandLine.text.toString())
            }
        }
    }

    private fun startNetcatSessionActivity(cmd: String) {
        val launchNetcatSession = Intent(this, NetcatSession::class.java)
        launchNetcatSession.putExtra(netcat_cmd_string, cmd)
        startActivity(launchNetcatSession)
    }
}
