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

    private fun showErrorToast(text: Int) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun validatePortRange(port: Int): Boolean {
        return port in 1..65535
    }

    private fun isValidHostString(host: String): Boolean {
        return host.matches(Regex(IP_REGEXP)) or host.matches(Regex(DOMAIN_REGEXP))
    }

    private fun isValidPortString(port: String): Boolean {
        try {
            port.toInt()
            return true
        } catch (e: NumberFormatException) {
            return false
        }
    }

    private fun validateAndGetSessionArgs(ncCmdText: String) : SessionArgs? {
        val args = LinkedList(ncCmdText.split(" "))
        if (args.pop() != "nc") {
            showErrorToast(R.string.error_missing_nc)
            return null
        }
        var host: String? = null
        var port: Int? = null
        var listen = false
        var proto = Proto.TCP
        var lineEnd = "\n"

        var expectPort = false
        for (arg in args) {
            if (isValidHostString(arg)) {
                if (host != null) {
                    showErrorToast(R.string.error_multiple_host)
                    return null
                }
                host = arg
            } else if (arg.startsWith("-")) {
                val flags = arg.substring(1).toCharArray()
                for (i in 0..flags.size - 1) {
                    if (expectPort) {
                        showErrorToast(R.string.error_expected_port)
                        return null
                    }
                    when (flags[i]) {
                        'l' -> listen = true
                        'u' -> proto = Proto.UDP
                        'p' -> expectPort = true
                        'C' -> lineEnd = "\r\n"
                    }
                }
            } else if (expectPort && !isValidPortString(arg)) {
                showErrorToast(R.string.error_expected_port)
                return null
            } else if (expectPort || isValidPortString(arg)) {
                port = arg.toInt()
                if (!validatePortRange(port)) {
                    showErrorToast(R.string.error_port_range)
                    return null
                }
            }
        }
        if ((host != null && port != null) || (host == null && port != null && listen)) {
            return SessionArgs(host, port, listen, proto, lineEnd)
        }
        showErrorToast(R.string.error_wrong_syntax)
        return null
    }

    private fun startNetcatSessionActivity(args: SessionArgs) {
        val launchNetcatSession = Intent(this, NetcatSession::class.java)
        launchNetcatSession.putExtra(netcat_cmd_extra, args)
        launchNetcatSession.putExtra(netcat_cmd_string, ncCmdText)
        startActivity(launchNetcatSession)
    }
}
