package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.io.IOException
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
    private val tView: TextView,
    private val sendQueue: LinkedList<String>
) : Thread() {

    var updateUIHandler: Handler = Handler(Looper.getMainLooper())

    class UpdateTextView(private val t: String, private val v: TextView) : Runnable {
        override fun run() {
            val nText: String = v.text.toString() + t
            v.text = nText
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
        try {
            val socketChannel: SocketChannel = SocketChannel.open()
            socketChannel.configureBlocking(false)
            socketChannel.connect(InetSocketAddress(sessionArgs.host, sessionArgs.port))
            while (!socketChannel.finishConnect()) {
                sleep(200)
            }
            updateUIHandler.post(UpdateTextView(
                "${sessionArgs.host} ${sessionArgs.port} open\n", tView))
            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                sendFromQueue(socketChannel)
                readFromChannel(socketChannel, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun startTCPListener() {
        try {
            val serverSocket: ServerSocketChannel = ServerSocketChannel.open()
            serverSocket.socket().bind(InetSocketAddress(sessionArgs.port))
            updateUIHandler.post(UpdateTextView(
                "Listening on ${serverSocket.socket().localSocketAddress}\n", tView))
            val clientChannel: SocketChannel = serverSocket.accept()
            clientChannel.configureBlocking(false)
            updateUIHandler.post(UpdateTextView(
                "Received connection from ${clientChannel.socket().remoteSocketAddress}\n",
                tView))
            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                sendFromQueue(clientChannel)
                readFromChannel(clientChannel, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun openUDPConnection() {
        try {
            val datagramChannel: DatagramChannel = DatagramChannel.open()
            datagramChannel.configureBlocking(false)
            datagramChannel.connect(InetSocketAddress(sessionArgs.host, sessionArgs.port))
            val buffer: ByteBuffer = ByteBuffer.allocate(65535)
            while (true) {
                sendFromQueue(datagramChannel)
                readFromChannel(datagramChannel, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun startUDPListener() {
        try {
            val datagramChannel: DatagramChannel = DatagramChannel.open()
            datagramChannel.socket().bind(InetSocketAddress(sessionArgs.port))
            val buffer: ByteBuffer = ByteBuffer.allocate(65535)
            val clientAddress: SocketAddress = datagramChannel.receive(buffer)
            updateUIHandler.post(UpdateTextView("Received datagram from $clientAddress\n", tView))
            updateUIHandler.post(UpdateTextView(
                String(buffer.array().slice(IntRange(0, buffer.position())).toByteArray()), tView))
            datagramChannel.connect(clientAddress)
            datagramChannel.configureBlocking(false)
            while (true) {
                sendFromQueue(datagramChannel)
                readFromChannel(datagramChannel, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun readFromChannel(socketChannel: ByteChannel, buffer: ByteBuffer) {
        val intRead: Int = socketChannel.read(buffer)
        if (intRead > 0) {
            updateUIHandler.post(UpdateTextView(
                String(buffer.array().slice(IntRange(0, intRead - 1)).toByteArray()), tView))
        }
    }

    private fun sendFromQueue(socketChannel: ByteChannel) {
        while (!sendQueue.isEmpty()) {
            val msg = "${sendQueue.pop()}${sessionArgs.lineEnd}"
            val sendBuf = ByteBuffer.wrap(msg.toByteArray())
            while (sendBuf.hasRemaining()) {
                socketChannel.write(sendBuf)
            }
            updateUIHandler.post(UpdateTextView(msg, tView))
        }
    }

    private fun handlerIOException(e: IOException) {
        if (e.message != null) {
            updateUIHandler.post(UpdateTextView("${e.message}\n", tView))
        }
    }
}