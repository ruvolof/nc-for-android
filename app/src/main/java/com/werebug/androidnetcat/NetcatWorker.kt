package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.Data
import android.widget.TextView
import java.io.*
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*

class NetcatWorker(
    private val sessionArgs: AndroidNetcatHome.SessionArgs,
    private val mainView: TextView
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
        updateUIHandler.post(AppendToTextView(message, mainView))
    }

    private fun updateViewOnException(e: IOException) {
        if (e.message != null) {
            updateMainView("${e.message}\n")
        }
    }

    override fun run() {
        val host = sessionArgs.host
        val proto = sessionArgs.proto
        if (host != null && proto == AndroidNetcatHome.Proto.TCP) {
            openTCPConnection()
        } else if (host == null && proto == AndroidNetcatHome.Proto.TCP) {
            startTCPListener()
        } else if (host != null && proto == AndroidNetcatHome.Proto.UDP) {
            openUDPConnection()
        } else if (host == null && proto == AndroidNetcatHome.Proto.UDP) {
            startUDPListener()
        }
    }

    private fun openTCPConnection() {
        var socketChannel: SocketChannel? = null
        try {
            socketChannel = SocketChannel.open()
            socketChannel.connect(InetSocketAddress(sessionArgs.host, sessionArgs.port))
            socketChannel.finishConnect()
            updateMainView("${sessionArgs.host} ${sessionArgs.port} open\n")
            socketChannel.configureBlocking(false)
            if (sessionArgs.exec == null) {
                startTwoWayChannel(socketChannel)
            } else {
                redirectProcessStreamsToSocket(sessionArgs.exec, socketChannel)
            }
        } catch (e: IOException) {
            updateViewOnException(e)
        } finally {
            socketChannel?.close()
        }
    }

    private fun startTCPListener() {
        var serverSocket: ServerSocketChannel? = null
        var clientChannel: SocketChannel? = null
        try {

            serverSocket = ServerSocketChannel.open()
            serverSocket.socket().bind(InetSocketAddress(sessionArgs.port))
            updateMainView("Listening on ${serverSocket.socket().localSocketAddress}\n")
            clientChannel = serverSocket.accept()
            clientChannel.configureBlocking(false)
            updateMainView(
                "Received connection from ${clientChannel.socket().remoteSocketAddress}\n"
            )
            if (sessionArgs.exec == null) {
                startTwoWayChannel(clientChannel)
            } else {
                redirectProcessStreamsToSocket(sessionArgs.exec, clientChannel)
            }
        } catch (e: IOException) {
            updateViewOnException(e)
        } finally {
            clientChannel?.close()
            serverSocket?.close()
        }
    }

    private fun openUDPConnection() {
        var datagramChannel: DatagramChannel? = null
        try {
            datagramChannel = DatagramChannel.open()
            datagramChannel.configureBlocking(false)
            datagramChannel.connect(InetSocketAddress(sessionArgs.host, sessionArgs.port))
            startTwoWayChannel(datagramChannel)
        } catch (e: IOException) {
            updateViewOnException(e)
        } finally {
            datagramChannel?.close()
        }
    }

    private fun startUDPListener() {
        var datagramChannel: DatagramChannel? = null
        try {
            datagramChannel = DatagramChannel.open()
            datagramChannel.socket().bind(InetSocketAddress(sessionArgs.port))
            updateMainView("Listening on ${datagramChannel.socket().localSocketAddress}\n")
            val buffer: ByteBuffer = ByteBuffer.allocate(65535)
            val clientAddress: SocketAddress = datagramChannel.receive(buffer)
            updateMainView("Received datagram from $clientAddress\n")
            updateMainView(
                String(
                    buffer.array().slice(IntRange(0, buffer.position())).toByteArray()
                )
            )
            datagramChannel.connect(clientAddress)
            datagramChannel.configureBlocking(false)
            startTwoWayChannel(datagramChannel)
        } catch (e: IOException) {
            updateViewOnException(e)
        } finally {
            datagramChannel?.close()
        }
    }

    private fun readBytes(socketChannel: ByteChannel): ByteArray {
        val buffer = ByteBuffer.allocate(65535)
        val readCount = socketChannel.read(buffer)
        if (readCount == -1) {
            throw IOException("Connection closed by the remote host.")
        }
        if (readCount > 0) {
            return buffer.array().slice(IntRange(0, readCount - 1)).toByteArray()
        }
        return byteArrayOf()
    }

    private fun readAndUpdateView(socketChannel: ByteChannel) {
        val bytes = readBytes(socketChannel)
        if (bytes.isNotEmpty()) {
            updateUIHandler.post(AppendToTextView(String(bytes), mainView))
        }
    }

    private fun sendBytes(channel: ByteChannel, bytes: ByteArray) {
        val sendBuffer = ByteBuffer.wrap(bytes)
        while (sendBuffer.hasRemaining()) {
            channel.write(sendBuffer)
        }
    }

    private fun sendFromUserInputQueue(socketChannel: ByteChannel) {
        while (!sendQueue.isEmpty()) {
            val msg = "${sendQueue.pop()}${sessionArgs.lineEnd}"
            sendBytes(socketChannel, msg.toByteArray())
            updateUIHandler.post(AppendToTextView(msg, mainView))
        }
    }

    private fun startTwoWayChannel(channel: ByteChannel) {
        while (!isStopped) {
            sendFromUserInputQueue(channel)
            readAndUpdateView(channel)
        }
    }

    private fun redirectProcessStreamsToSocket(command: String, socket: ByteChannel) {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val processStdout = process.inputStream
        val processStdin = process.outputStream
        var exited = false
        while (!exited && !isStopped) {
            val outputByteCount = processStdout.available()
            if (outputByteCount > 0) {
                val bytes = ByteArray(outputByteCount)
                processStdout.read(bytes)
                sendBytes(socket, bytes)
            }
            val inputFromSocket = readBytes(socket)
            if (inputFromSocket.isNotEmpty()) {
                processStdin.write(inputFromSocket)
                processStdin.flush()
            }
            try {
                process.exitValue()
                exited = true
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