package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.io.*
import java.util.*

class NetcatWorker(
    private val ncArgv: List<String>,
    private val connectionOutputView: TextView
) : Thread() {

    private val sendQueue = LinkedList<String>()
    private val updateUIHandler: Handler = Handler(Looper.getMainLooper())
    private var isStopped = false

    class AppendToTextView(private val message: String, private val view: TextView) : Runnable {
        override fun run() {
            val newText = "${view.text}${message}"
            view.text = newText
        }
    }

    private fun updateMainView(message: String) {
        updateUIHandler.post(AppendToTextView(message, connectionOutputView))
    }

    private fun updateViewOnException(e: IOException) {
        if (e.message != null) {
            updateMainView("${e.message}\n")
        }
    }

    override fun run() {
        execNcatProcess(ncArgv)
    }

    private fun execNcatProcess(command: List<String>) {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val processStdout = process.inputStream
        val processStdin = process.outputStream
        var exited = false
        while (!exited && !isStopped) {
            while (!sendQueue.isEmpty()) {
                val msg = "${sendQueue.pop()}\n"
                processStdin.write(msg.toByteArray())
                processStdin.flush()
                updateMainView(msg)
            }
            val outputByteCount = processStdout.available()
            if (outputByteCount > 0) {
                val bytes = ByteArray(outputByteCount)
                processStdout.read(bytes)
                updateMainView(String(bytes))
            }
            try {
                val exitValue = process.exitValue()
                exited = true
                updateMainView("\n\nNcat command finished. Exit value: $exitValue.")
            } catch (_: IllegalThreadStateException) {
            }
        }
        if (!exited) {
            process.destroy()
        }
    }

    fun addToSendQueue(message: String) {
        sendQueue.add(message)
    }

    fun halt() {
        isStopped = true
    }
}