package com.werebug.androidnetcat

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*

class NetcatWorker(
    private val host: String?,
    private val port: Int,
    private val proto: AndroidNetcatHome.Proto,
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
            val cSocket: SocketChannel = SocketChannel.open()
            cSocket.configureBlocking(false)

            cSocket.connect(InetSocketAddress(host, port))

            while (!cSocket.finishConnect()) {
                sleep(200)
            }

            val connectionSuccessful = "$host $port open\n"
            updateUIHandler.post(UpdateTextView(connectionSuccessful, tView))

            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                // Checking queue for messages to send
                sendMessages(cSocket)

                // Continuously reading from socket
                readMessages(cSocket, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun startTCPListener() {
        try {
            val sSocket: ServerSocketChannel = ServerSocketChannel.open()
            sSocket.socket().bind(InetSocketAddress(port))

            val listeningSuccess: String =
                "Listening on " + sSocket.socket().localSocketAddress + "\n"
            updateUIHandler.post(UpdateTextView(listeningSuccess, tView))

            val cSocket: SocketChannel = sSocket.accept()
            cSocket.configureBlocking(false)

            val connectionReceived: String =
                "Received connection from " + cSocket.socket().remoteSocketAddress.toString() + "\n"
            updateUIHandler.post(UpdateTextView(connectionReceived, tView))

            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            while (true) {
                // Checking queue for messages to send
                sendMessages(cSocket)

                // Continuously reading from socket
                readMessages(cSocket, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun openUDPConnection() {
        try {
            val dSocket: DatagramChannel = DatagramChannel.open()
            dSocket.configureBlocking(false)

            dSocket.connect(InetSocketAddress(host, port))

            val buffer: ByteBuffer = ByteBuffer.allocate(65535)
            while (true) {
                // Checking queue for messages to send
                sendDatagram(dSocket)

                // Continuously reading from socket
                readDatagram(dSocket, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun startUDPListener() {
        try {
            val dSocket: DatagramChannel = DatagramChannel.open()
            dSocket.socket().bind(InetSocketAddress(port))

            val buffer: ByteBuffer = ByteBuffer.allocate(65535)

            val clientAddress: SocketAddress = dSocket.receive(buffer)

            val receivedDatagram = "Received datagram from $clientAddress\n"
            updateUIHandler.post(UpdateTextView(receivedDatagram, tView))

            val bufferArr = buffer.array()
            val bufferStr = String(bufferArr.slice(IntRange(0, buffer.position())).toByteArray())
            updateUIHandler.post(UpdateTextView(bufferStr, tView))

            dSocket.connect(clientAddress)
            dSocket.configureBlocking(false)

            while (true) {
                // Checking queue for messages to send
                sendDatagram(dSocket)

                // Continuously reading from socket
                readDatagram(dSocket, buffer)
                buffer.clear()
            }
        } catch (e: IOException) {
            handlerIOException(e)
        }
    }

    private fun readDatagram(dSocket: DatagramChannel, buffer: ByteBuffer) {
        val intRead: Int = dSocket.read(buffer)
        if (intRead > 0) {
            val bufferArr = buffer.array()
            val bufferStr = String(bufferArr.slice(IntRange(0, intRead - 1)).toByteArray())
            updateUIHandler.post(UpdateTextView(bufferStr, tView))
        }
    }

    private fun sendDatagram(dSocket: DatagramChannel) {
        while (!sendQueue.isEmpty()) {
            val msg: String = sendQueue.pop() + "\n"
            val sendBuf = ByteBuffer.wrap(msg.toByteArray())
            while (sendBuf.hasRemaining()) {
                dSocket.write(sendBuf)
            }
            updateUIHandler.post(UpdateTextView(msg, tView))
        }
    }

    private fun sendMessages(c_socket: SocketChannel) {
        while (!sendQueue.isEmpty()) {
            val msg: String = sendQueue.pop() + "\n"
            val sendBuf = ByteBuffer.wrap(msg.toByteArray())
            while (sendBuf.hasRemaining()) {
                c_socket.write(sendBuf)
            }
            updateUIHandler.post(UpdateTextView(msg, tView))
        }
    }

    private fun readMessages(c_socket: SocketChannel, buffer: ByteBuffer) {
        val intRead: Int = c_socket.read(buffer)
        if (intRead > 0) {
            val bufferArr = buffer.array()
            val bufferStr = String(bufferArr.slice(IntRange(0, intRead - 1)).toByteArray())
            updateUIHandler.post(UpdateTextView(bufferStr, tView))
        }
    }

    private fun handlerIOException(e: IOException) {
        val error: String? = e.message
        if (error != null) {
            updateUIHandler.post(UpdateTextView(error + "\n", tView))
        }
    }
}