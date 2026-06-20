package com.mobilegamecontroller.data.remote

import com.mobilegamecontroller.di.IoDispatcher
import com.mobilegamecontroller.domain.model.ConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-latency TCP client for real-time controller input streaming.
 *
 * Performance optimizations:
 * - TCP_NODELAY disables Nagle's algorithm for immediate packet send
 * - BufferedWriter with autoFlush=false; manual flush only when needed
 * - Dedicated IO dispatcher keeps network off main thread
 * - Newline-delimited JSON for simple framing without overhead
 */
@Singleton
class TcpSocketClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latencyMs = MutableStateFlow(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var scope: CoroutineScope? = null
    private var pingJob: Job? = null
    private var readJob: Job? = null
    private var reconnectJob: Job? = null

    private var lastHost: String? = null
    private var lastPort: Int? = null
    private var autoReconnectEnabled = true
    private var userDisconnect = false

    /** Pending ping timestamp for RTT calculation. */
    private var pendingPingTime = 0L

    /** Serializes all socket writes; BufferedWriter is not thread-safe. */
    private val writeMutex = Mutex()

    suspend fun connect(host: String, port: Int, scope: CoroutineScope): Result<Unit> =
        withContext(ioDispatcher) {
            userDisconnect = false
            lastHost = host
            lastPort = port
            this@TcpSocketClient.scope = scope
            connectInternal(host, port)
        }

    private suspend fun connectInternal(host: String, port: Int): Result<Unit> {
        _connectionState.value = ConnectionState.CONNECTING
        return try {
            disconnectInternal(notify = false)

            val newSocket = Socket()
            // 3 second connect timeout
            newSocket.connect(InetSocketAddress(host, port), 3000)
            // Critical: disable Nagle's algorithm for low latency
            newSocket.tcpNoDelay = true
            newSocket.keepAlive = true
            newSocket.soTimeout = 0

            socket = newSocket
            writer = BufferedWriter(OutputStreamWriter(newSocket.getOutputStream(), Charsets.UTF_8))
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream(), Charsets.UTF_8))

            _connectionState.value = ConnectionState.CONNECTED
            startPingLoop()
            startReadLoop()
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnect()
            Result.failure(e)
        }
    }

    /**
     * Send raw JSON line to server. Serialized via [writeMutex] because
     * press/release and ping can otherwise corrupt the same BufferedWriter.
     */
    suspend fun send(message: String, flushImmediately: Boolean = true): Result<Unit> =
        withContext(ioDispatcher) {
            writeMutex.withLock {
                try {
                    val w = writer ?: return@withLock Result.failure(IOException("Not connected"))
                    w.write(message)
                    if (flushImmediately) {
                        w.flush()
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    handleDisconnect()
                    Result.failure(e)
                }
            }
        }

    fun disconnect() {
        userDisconnect = true
        autoReconnectEnabled = false
        reconnectJob?.cancel()
        scope?.launch(ioDispatcher) {
            disconnectInternal()
        }
    }

    fun enableAutoReconnect() {
        autoReconnectEnabled = true
        userDisconnect = false
    }

    private suspend fun disconnectInternal(notify: Boolean = true) {
        pingJob?.cancel()
        readJob?.cancel()
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (_: Exception) {
            // Ignore close errors
        }
        writer = null
        reader = null
        socket = null
        if (notify) {
            _connectionState.value = ConnectionState.DISCONNECTED
            _latencyMs.value = -1L
        }
    }

    private fun handleDisconnect() {
        if (_connectionState.value != ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.DISCONNECTED
        _latencyMs.value = -1L
        pingJob?.cancel()
        readJob?.cancel()

        val coroutineScope = scope ?: return
        coroutineScope.launch(ioDispatcher) {
            disconnectInternal(notify = false)
            scheduleReconnect()
        }
    }

    /** Auto reconnect with exponential backoff: 1s, 2s, 4s, max 8s */
    private fun scheduleReconnect() {
        if (!autoReconnectEnabled || userDisconnect) return
        val host = lastHost ?: return
        val port = lastPort ?: return
        val coroutineScope = scope ?: return

        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch(ioDispatcher) {
            _connectionState.value = ConnectionState.RECONNECTING
            var backoffMs = 1000L
            while (isActive && !userDisconnect && autoReconnectEnabled) {
                delay(backoffMs)
                val result = connectInternal(host, port)
                if (result.isSuccess) return@launch
                backoffMs = (backoffMs * 2).coerceAtMost(8000L)
            }
        }
    }

    /** Ping every 2 seconds to measure round-trip latency. */
    private fun startPingLoop() {
        val coroutineScope = scope ?: return
        pingJob?.cancel()
        pingJob = coroutineScope.launch(ioDispatcher) {
            while (isActive && socket?.isConnected == true) {
                pendingPingTime = System.currentTimeMillis()
                send(ControllerJson.encodePing(), flushImmediately = true)
                delay(2000)
            }
        }
    }

    /** Read pong responses from server for latency measurement. */
    private fun startReadLoop() {
        val coroutineScope = scope ?: return
        readJob?.cancel()
        readJob = coroutineScope.launch(ioDispatcher) {
            try {
                val r = reader ?: return@launch
                while (isActive) {
                    val line = r.readLine() ?: break
                    if (line.contains("\"pong\"")) {
                        _latencyMs.value = System.currentTimeMillis() - pendingPingTime
                    }
                }
                handleDisconnect()
            } catch (e: CancellationException) {
                throw e
            } catch (_: SocketException) {
                handleDisconnect()
            } catch (_: IOException) {
                handleDisconnect()
            }
        }
    }
}
