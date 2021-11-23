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
        const val netcat_cmd_extra: String = "NETCAT_CMD"
        const val netcat_cmd_string: String = "NETCAT_CMD_STRING"
        const val IP_REGEXP = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"
        const val DOMAIN_REGEXP = "^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}\$"
    }

    private lateinit var btnStartNetcat: ImageButton
    private lateinit var ncCommandLineEdittext: EditText
    private var ncCmdText: String? = null

    enum class Proto {
        TCP,
        UDP
    }

    class SessionArgs(val host: String?, val port: Int, val listen: Boolean, val proto: Proto) :
        Serializable {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netcat_home)

        btnStartNetcat = findViewById(R.id.btn_start_netcat)
        btnStartNetcat.setOnClickListener(this)

        ncCommandLineEdittext = findViewById(R.id.et_nc_command_line)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_netcat -> {
                ncCmdText = ncCommandLineEdittext.text.toString()
                checkCommandSyntax(ncCmdText as String)
            }
        }
    }

    private fun checkCommandSyntax(ncCmdText: String) {
        val args = LinkedList(ncCmdText.split(" "))
        if (args.pop() != "nc") {
            Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
            return
        }

        var host: String? = null
        var port: Int? = null
        var listen = false
        var proto: Proto = Proto.TCP

        var expectPort = false

        for (arg in args) {
            if (arg.matches(Regex(IP_REGEXP)) or arg.matches(Regex(DOMAIN_REGEXP))) {
                if (host != null) {
                    Toast.makeText(
                        this, "Only one host allowed. See examples.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                host = arg
            } else if (arg.startsWith("-")) {
                val nit: String = arg.substring(1)
                val vArr = nit.toCharArray()
                vArr.forEach {
                    when (it) {
                        'l' -> listen = true
                        'u' -> proto = Proto.UDP
                        'p' -> expectPort = true
                    }
                }
            } else if (expectPort && !arg.matches(Regex("^\\d{1,5}$"))) {
                Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
                return
            } else if (expectPort || arg.matches(Regex("^\\d{1,5}$"))) {
                port = arg.toInt()
                if (!checkPortRange(port)) {
                    Toast.makeText(this, R.string.wrong_port_range, Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        if ((host != null && port != null) || (host == null && port != null && listen)) {
            val sessionArgs = SessionArgs(host, port as Int, listen, proto)
            startNetcatSessionActivity(sessionArgs)
        } else {
            Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPortRange(port: Int): Boolean {
        return port in 1..65535
    }

    private fun startNetcatSessionActivity(args: SessionArgs) {
        val launchNetcatSession = Intent(this, NetcatSession::class.java)
        launchNetcatSession.putExtra(netcat_cmd_extra, args)
        launchNetcatSession.putExtra(netcat_cmd_string, ncCmdText)
        startActivity(launchNetcatSession)
    }
}
