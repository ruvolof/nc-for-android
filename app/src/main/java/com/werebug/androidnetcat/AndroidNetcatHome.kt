package com.werebug.androidnetcat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.werebug.androidnetcat.databinding.ActivityNetcatHomeBinding
import java.io.Serializable
import java.util.*

class AndroidNetcatHome : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val netcat_cmd_extra: String = "NETCAT_CMD"
        const val netcat_cmd_string: String = "NETCAT_CMD_STRING"
        const val IP_REGEXP = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"
        const val DOMAIN_REGEXP = "^([a-zA-Z0-9]+(-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}\$"
    }

    private lateinit var binding: ActivityNetcatHomeBinding;
    private var ncCmdText: String = ""

    enum class Proto {
        TCP,
        UDP
    }

    class SessionArgs(
        val host: String?,
        val port: Int,
        val listen: Boolean,
        val proto: Proto,
        val lineEnd: String = "\n") : Serializable {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetcatHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartNetcat.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start_netcat -> {
                ncCmdText = binding.etNcCommandLine.text.toString()
                val sessionArgs = validateAndGetSessionArgs(ncCmdText)
                if (sessionArgs != null) {
                    startNetcatSessionActivity(sessionArgs)
                }
            }
        }
    }

    private fun validateAndGetSessionArgs(ncCmdText: String) : SessionArgs? {
        val args = LinkedList(ncCmdText.split(" "))
        if (args.pop() != "nc") {
            Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
            return null
        }

        var host: String? = null
        var port: Int? = null
        var listen = false
        var proto = Proto.TCP
        var lineEnd = "\n"

        var expectPort = false

        for (arg in args) {
            if (arg.matches(Regex(IP_REGEXP)) or arg.matches(Regex(DOMAIN_REGEXP))) {
                if (host != null) {
                    Toast.makeText(
                        this, "Only one host allowed. See examples.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
                host = arg
            } else if (arg.startsWith("-")) {
                val flags = arg.substring(1).toCharArray()
                flags.forEach {
                    when (it) {
                        'l' -> listen = true
                        'u' -> proto = Proto.UDP
                        'p' -> expectPort = true
                        'C' -> lineEnd = "\r\n"
                    }
                }
            } else if (expectPort && !arg.matches(Regex("^\\d{1,5}$"))) {
                Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
                return null
            } else if (expectPort || arg.matches(Regex("^\\d{1,5}$"))) {
                port = arg.toInt()
                if (!checkPortRange(port)) {
                    Toast.makeText(this, R.string.wrong_port_range, Toast.LENGTH_SHORT).show()
                    return null
                }
            }
        }

        if ((host != null && port != null) || (host == null && port != null && listen)) {
            return SessionArgs(host, port, listen, proto, lineEnd)
        }
        Toast.makeText(this, R.string.nc_wrong_syntax, Toast.LENGTH_SHORT).show()
        return null
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
