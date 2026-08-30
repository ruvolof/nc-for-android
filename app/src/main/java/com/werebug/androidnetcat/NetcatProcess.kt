package com.werebug.androidnetcat

import java.util.concurrent.ConcurrentLinkedQueue

class NetcatProcess(
    private val ncArgv: List<String>,
    private val listener: Listener
) {

    interface Listener {
        fun onOutput(message: String)
        fun onExit(exitValue: Int)
    }

    private val sendQueue = ConcurrentLinkedQueue<String>()

    @Volatile
    private var isStopped = false

    fun run() {
        val process = ProcessBuilder(ncArgv).redirectErrorStream(true).start()
        val processStdout = process.inputStream
        val processStdin = process.outputStream
        var exited = false
        while (!exited && !isStopped) {
            var dataProcessed = false
            while (true) {
                val queued = sendQueue.poll() ?: break
                val msg = "$queued\n"
                processStdin.write(msg.toByteArray())
                processStdin.flush()
                listener.onOutput(msg)
                dataProcessed = true
            }
            val outputByteCount = processStdout.available()
            if (outputByteCount > 0) {
                val bytes = ByteArray(outputByteCount)
                processStdout.read(bytes)
                listener.onOutput(String(bytes))
                dataProcessed = true
            }
            try {
                val exitValue = process.exitValue()
                exited = true
                listener.onOutput("\n\nNcat command finished. Exit value: $exitValue.")
                listener.onExit(exitValue)
            } catch (_: IllegalThreadStateException) {
                if (!dataProcessed) {
                    Thread.sleep(100)
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
