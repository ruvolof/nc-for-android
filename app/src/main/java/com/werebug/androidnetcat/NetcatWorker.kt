package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.*

class NetcatWorker(
    private val ncArgv: List<String>,
    private val sessionActivityRef: WeakReference<NetcatSession>
) : Thread() {

    private val sendQueue = LinkedList<String>()
    private val updateUIHandler: Handler = Handler(Looper.getMainLooper())
    private var isStopped = false

    private fun updateMainView(message: String) {
        updateUIHandler.post { sessionActivityRef.get()?.appendToOutputView(message) }
    }

    private fun disableMessageViews() {
        updateUIHandler.post { sessionActivityRef.get()?.disableMessageViews() }
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
            var dataProcessed = false
            while (!sendQueue.isEmpty()) {
                val msg = "${sendQueue.pop()}\n"
                processStdin.write(msg.toByteArray())
                processStdin.flush()
                updateMainView(msg)
                dataProcessed = true
            }
            val outputByteCount = processStdout.available()
            if (outputByteCount > 0) {
                val bytes = ByteArray(outputByteCount)
                processStdout.read(bytes)
                updateMainView(String(bytes))
                dataProcessed = true
            }
            try {
                val exitValue = process.exitValue()
                exited = true
                updateMainView("\n\nNcat command finished. Exit value: $exitValue.")
                disableMessageViews()
            } catch (_: IllegalThreadStateException) {
                if (!dataProcessed) {
                    sleep(100)
                }
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