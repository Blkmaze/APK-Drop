package com.example.network

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredDevice(
    val deviceName: String,
    val ipAddress: String,
    val port: Int = 8888,
    val activeCode: String = "",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class LanDiscovery(private val scope: CoroutineScope) {
    private val TAG = "LanDiscovery"
    private val DISCOVERY_PORT = 8999
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var socket: DatagramSocket? = null

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    fun startDiscovery(myIp: String, myPort: Int, currentCode: String) {
        stopDiscovery()

        val myDeviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        // Start listening
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)

                while (isActive && socket != null && !socket!!.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket!!.receive(packet)
                        val message = String(packet.data, 0, packet.length).trim()
                        val senderIp = packet.address.hostAddress ?: continue

                        // Ignore our own broadcast
                        if (senderIp == myIp) continue

                        if (message.startsWith("APK_DROP_BEACON|")) {
                            val parts = message.split("|")
                            if (parts.size >= 4) {
                                val name = parts[1]
                                val port = parts[2].toIntOrNull() ?: 8888
                                val code = if (parts.size > 4) parts[4] else ""

                                withContext(Dispatchers.Main) {
                                    val current = _discoveredDevices.value.toMutableList()
                                    val existingIdx = current.indexOfFirst { it.ipAddress == senderIp }
                                    val updatedDevice = DiscoveredDevice(
                                        deviceName = name,
                                        ipAddress = senderIp,
                                        port = port,
                                        activeCode = code,
                                        lastSeenTimestamp = System.currentTimeMillis()
                                    )
                                    if (existingIdx >= 0) {
                                        current[existingIdx] = updatedDevice
                                    } else {
                                        current.add(updatedDevice)
                                    }
                                    _discoveredDevices.value = current
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery listen error: ${e.message}")
            }
        }

        // Start periodic beacon broadcast
        broadcastJob = scope.launch(Dispatchers.IO) {
            try {
                val broadcastSocket = DatagramSocket().apply { broadcast = true }
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val message = "APK_DROP_BEACON|$myDeviceName|$myPort|$myIp|$currentCode"
                val data = message.toByteArray()

                while (isActive) {
                    try {
                        val packet = DatagramPacket(data, data.size, broadcastAddr, DISCOVERY_PORT)
                        broadcastSocket.send(packet)
                    } catch (_: Exception) {}
                    kotlinx.coroutines.delay(3500)
                }
                broadcastSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast error: ${e.message}")
            }
        }
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        broadcastJob = null
        listenJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
