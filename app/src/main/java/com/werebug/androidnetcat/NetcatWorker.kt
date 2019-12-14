package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*

class NetcatWorker(host: String?, port: Int, proto: AndroidNetcatHome.Proto, tView: TextView, sendQueue: LinkedList<String>) : Thread() {

    val host = host
    val port = port
    val proto = proto
    val tView = tView
    val sendQueue = sendQueue

    var updateUIHandler: Handler = Handler(Looper.getMainLooper())

    private val LogTag : String = "NetcatWorker"

    class updateTextView(t: String, v: TextView) : Runnable {
        val t = t
        val v = v

        override fun run() {
            val nText: String = v.text.toString() + t
            v.setText(nText)
        }
    }

    override fun run() {
        if (host != null && proto == AndroidNetcatHome.Proto.TCP) {
            openTCPConnection()
        }
        else if (host == null && proto == AndroidNetcatHome.Proto.TCP) {
            startTCPListener()
        }
        else if (host != null && proto == AndroidNetcatHome.Proto.UDP) {
            openUDPConnection()
        }
        else if (host == null && proto == AndroidNetcatHome.Proto.UDP) {
            startUDPListener()
        }
    }

    private fun openTCPConnection() {
        try {
            val c_socket: SocketChannel = SocketChannel.open()
            c_socket.configureBlocking(false)

            c_socket.connect(InetSocketAddress(host, port))

            while (!c_socket.finishConnect()) {
                sleep(200)
            }

            val connectionSuccessful: String = host + " " + port + " open\n"
            updateUIHandler.post(updateTextView(connectionSuccessful, tView))

            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                // Checking queue for messages to send
                sendMessages(c_socket)

                // Continuosly reading from socket
                readMessages(c_socket, buffer)
                buffer.clear()
            }
        }
        catch (e : IOException) {
            handlerIOException(e)
        }
    }

    private fun startTCPListener() {
        try {
            val s_socket: ServerSocketChannel = ServerSocketChannel.open()
            s_socket.socket().bind(InetSocketAddress(port))

            val listening_success: String = "Listening on " + s_socket.socket().localSocketAddress + "\n"
            updateUIHandler.post(updateTextView(listening_success, tView))

            val c_socket : SocketChannel = s_socket.accept()
            c_socket.configureBlocking(false)

            val connection_received: String = "Received connection from " + c_socket.socket().remoteSocketAddress.toString() + "\n"
            updateUIHandler.post(updateTextView(connection_received, tView))

            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                // Checking queue for messages to send
                sendMessages(c_socket)

                // Continuosly reading from socket
                readMessages(c_socket, buffer)
                buffer.clear()
            }
        }
        catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun openUDPConnection() {
        try {
            val d_socket: DatagramChannel = DatagramChannel.open()
            d_socket.configureBlocking(false)

            d_socket.connect(InetSocketAddress(host,port))

            val buffer: ByteBuffer = ByteBuffer.allocate(65535)
            while (true) {
                // Checking queue for messages to send
                sendDatagram(d_socket)

                // Continuosly reading from socket
                readDatagram(d_socket, buffer)
                buffer.clear()
            }
        }
        catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun startUDPListener() {
        try {
            val d_socket : DatagramChannel = DatagramChannel.open()
            d_socket.socket().bind(InetSocketAddress(port))

            val buffer: ByteBuffer = ByteBuffer.allocate(65535)

            val clientAddress : SocketAddress = d_socket.receive(buffer)

            val received_datagram: String = "Received datagram from " + clientAddress.toString() + "\n"
            updateUIHandler.post(updateTextView(received_datagram, tView))

            val buffer_arr = buffer.array()
            val buffer_str = String(buffer_arr.slice(IntRange(0, buffer.position())).toByteArray())
            updateUIHandler.post(updateTextView(buffer_str, tView))

            d_socket.connect(clientAddress)
            d_socket.configureBlocking(false)

            while (true) {
                // Checking queue for messages to send
                sendDatagram(d_socket)

                // Continuosly reading from socket
                readDatagram(d_socket, buffer)
                buffer.clear()
            }
        }
        catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun readDatagram(dSocket: DatagramChannel, buffer: ByteBuffer) {
        val intRead: Int = dSocket.read(buffer)
        if (intRead > 0) {
            val buffer_arr = buffer.array()
            val buffer_str = String(buffer_arr.slice(IntRange(0, intRead - 1)).toByteArray())
            updateUIHandler.post(updateTextView(buffer_str, tView))
        }
    }

    private fun sendDatagram(dSocket: DatagramChannel) {
        while (!sendQueue.isEmpty()) {
            val msg: String = sendQueue.pop() + "\n"
            val sendBuf : ByteBuffer = ByteBuffer.wrap(msg.toByteArray())
            while (sendBuf.hasRemaining()) {
                dSocket.write(sendBuf)
            }
            updateUIHandler.post(updateTextView(msg, tView))
        }
    }

    private fun sendMessages(c_socket: SocketChannel) {
        while (!sendQueue.isEmpty()) {
            val msg: String = sendQueue.pop() + "\n"
            val sendBuf : ByteBuffer = ByteBuffer.wrap(msg.toByteArray())
            while (sendBuf.hasRemaining()) {
                c_socket.write(sendBuf)
            }
            updateUIHandler.post(updateTextView(msg, tView))
        }
    }

    private fun readMessages(c_socket: SocketChannel, buffer: ByteBuffer) {
        val intRead: Int = c_socket.read(buffer)
        if (intRead > 0) {
            val buffer_arr = buffer.array()
            val buffer_str = String(buffer_arr.slice(IntRange(0, intRead - 1)).toByteArray())
            updateUIHandler.post(updateTextView(buffer_str, tView))
        }
    }

    private fun handlerIOException(e: IOException) {
        Log.d(LogTag, e.message)
        val error: String? = e.message
        if (error != null) {
            updateUIHandler.post(updateTextView(error + "\n", tView))
        }
    }
}