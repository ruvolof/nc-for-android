package com.werebug.androidnetcat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

class AndroidNetcatHome : AppCompatActivity(), View.OnClickListener {

    companion object {
        val netcat_cmd_extra: String = "NETCAT_CMD"
    }

    private val LogTag: String = "AndroidNetcatHome"
    private lateinit var btn_start_netcat: ImageButton
    private lateinit var nc_command_line_edittext: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netcat_home)

        btn_start_netcat = findViewById(R.id.btn_start_netcat)
        btn_start_netcat.setOnClickListener(this)

        nc_command_line_edittext = findViewById(R.id.et_nc_command_line)
    }

    override fun onClick(v: View?) {
        val nc_cmd_text: String = nc_command_line_edittext.text.toString()
        when (v?.id) {
            R.id.btn_start_netcat -> check_command_syntax(nc_cmd_text)
        }
    }

    private fun check_command_syntax(ncCmdText: String) {
        operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

        // Adding regexp for "nc 127.0.0.1 4567"
        val simple_tcp_connection_re = "^nc \\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} \\d{1,5}$"
        val simple_tcp_listener_re = "^nc -lp \\d{1,5}$"

        when (ncCmdText) {
            in Regex(simple_tcp_connection_re) -> {
                Log.d(LogTag, "Simple TCP syntax found.")
                val port: Int = ncCmdText.split(" ")[2].toInt()
                if (checkPortRange(port)) {
                    start_netcat_session_activity(ncCmdText)
                }
            }
            in Regex(simple_tcp_listener_re) -> {
                Log.d(LogTag, "Simple TCP Listener syntax found.")
                val port: Int = ncCmdText.split(" ")[2].toInt()
                if (checkPortRange(port)) {
                    start_netcat_session_activity(ncCmdText)
                }
            }
            else -> {
                Log.d(LogTag, "Wrong nc syntax.")
                Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPortRange(port: Int): Boolean {
        return port > 0 && port <= 65535
    }

    private fun start_netcat_session_activity(ncCmdText: String) {
        Log.d(LogTag, "Launching NetcatSession Activity with: " + ncCmdText)

        val launch_netcat_session = Intent(this, NetcatSession::class.java)
        launch_netcat_session.putExtra(netcat_cmd_extra, ncCmdText)
        startActivity(launch_netcat_session)
    }
}
