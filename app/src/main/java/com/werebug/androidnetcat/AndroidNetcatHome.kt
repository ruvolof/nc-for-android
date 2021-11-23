package com.werebug.androidnetcat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import java.io.Serializable
import java.util.*

class AndroidNetcatHome : AppCompatActivity(), View.OnClickListener {

    companion object {
        val netcat_cmd_extra: String = "NETCAT_CMD"
        val netcat_cmd_string: String = "NETCAT_CMD_STRING"
    }

    private val LogTag: String = "AndroidNetcatHome"
    private lateinit var btn_start_netcat: ImageButton
    private lateinit var nc_command_line_edittext: EditText
    private var nc_cmd_text: String? = null

    enum class Proto {
        TCP,
        UDP
    }

    class sessionArgs(host: String?, port: Int, listen: Boolean, proto: Proto) : Serializable {
        val host = host
        val port = port
        val listen = listen
        val proto = proto
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netcat_home)

        btn_start_netcat = findViewById(R.id.btn_start_netcat)
        btn_start_netcat.setOnClickListener(this)

        nc_command_line_edittext = findViewById(R.id.et_nc_command_line)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_netcat -> {
                nc_cmd_text = nc_command_line_edittext.text.toString();
                check_command_syntax(nc_cmd_text as String);
            }
        }
    }

    private fun check_command_syntax(ncCmdText: String) {
        val args : LinkedList<String> = LinkedList(ncCmdText.split(" "))
        if (args.pop() != "nc") {
            Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
            return
        }

        var host: String? = null
        var port: Int? = null
        var listen: Boolean = false
        var proto: Proto = Proto.TCP

        var expect_port: Boolean = false


        args.forEach () {
            if (it.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
                host = it
            }
            else if (it.matches(
                        Regex("^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}\$"))) {
                host = it
            }
            else if (it.startsWith("-")) {
                var nit : String = it.substring(1)
                var v_arr = nit.toCharArray()
                v_arr.forEach {
                    when (it) {
                        'l' -> listen = true
                        'u' -> proto = Proto.UDP
                        'p' -> expect_port = true
                    }
                }
            }
            else if (expect_port && !it.matches(Regex("^\\d{1,5}$"))) {
                Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
                return
            }
            else if (expect_port || it.matches(Regex("^\\d{1,5}$"))) {
                port = it.toInt()
                if (port!! < 1 || port!! > 65535) {
                    Toast.makeText(this, R.string.wrong_port_range, Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        if ((host != null && port != null) || (host == null && port != null && listen)) {
            val sargs: sessionArgs = sessionArgs(host, port as Int, listen, proto)
            start_netcat_session_activity(sargs)
        }
        else {
            Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPortRange(port: Int): Boolean {
        return port > 0 && port <= 65535
    }

    private fun start_netcat_session_activity(sargs: sessionArgs) {
        val launch_netcat_session = Intent(this, NetcatSession::class.java)
        launch_netcat_session.putExtra(netcat_cmd_extra, sargs)
        launch_netcat_session.putExtra(netcat_cmd_string, nc_cmd_text)
        startActivity(launch_netcat_session)
    }
}
