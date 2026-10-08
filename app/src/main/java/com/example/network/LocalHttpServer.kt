package com.example.network

import android.util.Log
import com.example.data.model.ApkItem
import com.example.data.repository.ApkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

data class ServerStatus(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = 8888,
    val connectedClientsCount: Int = 0,
    val totalTransferredBytes: Long = 0,
    val lastEvent: String = "Server stopped"
)

class LocalHttpServer(
    private val apkRepository: ApkRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "LocalHttpServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _status = MutableStateFlow(ServerStatus())
    val status = _status.asStateFlow()

    fun start(ipAddress: String, port: Int = 8888) {
        if (_status.value.isRunning) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                _status.value = _status.value.copy(
                    isRunning = true,
                    ipAddress = ipAddress,
                    port = port,
                    lastEvent = "Server running at http://$ipAddress:$port"
                )
                Log.d(TAG, "Server started on port $port")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        launch(Dispatchers.IO) {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server: ${e.message}")
                _status.value = _status.value.copy(
                    isRunning = false,
                    lastEvent = "Error: ${e.message}"
                )
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        _status.value = _status.value.copy(
            isRunning = false,
            lastEvent = "Server stopped"
        )
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            _status.value = _status.value.copy(
                connectedClientsCount = _status.value.connectedClientsCount + 1
            )

            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val rawOutput = socket.getOutputStream()
            val output = BufferedOutputStream(rawOutput)

            val requestLine = input.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext
            val method = parts[0]
            val path = URLDecoder.decode(parts[1], "UTF-8")

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String? = input.readLine()
            while (!line.isNullOrEmpty()) {
                val idx = line.indexOf(":")
                if (idx > 0) {
                    val key = line.substring(0, idx).trim().lowercase()
                    val value = line.substring(idx + 1).trim()
                    headers[key] = value
                }
                line = input.readLine()
            }

            when {
                path == "/" || path == "/index.html" -> {
                    serveHtmlIndex(output)
                }
                path.startsWith("/apk/") -> {
                    val idStr = path.removePrefix("/apk/")
                    val id = idStr.toLongOrNull()
                    if (id != null) {
                        serveApkById(id, output)
                    } else {
                        send404(output)
                    }
                }
                path.startsWith("/code/") -> {
                    val code = path.removePrefix("/code/").trim()
                    serveApkByCode(code, output)
                }
                path == "/api/list" -> {
                    serveApiList(output)
                }
                path == "/upload" && method.equals("POST", ignoreCase = true) -> {
                    handleUpload(headers, socket.getInputStream(), output)
                }
                else -> {
                    send404(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client request: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
            _status.value = _status.value.copy(
                connectedClientsCount = maxOf(0, _status.value.connectedClientsCount - 1)
            )
        }
    }

    private suspend fun serveHtmlIndex(output: BufferedOutputStream) {
        val vaultDir = apkRepository.getVaultDirectory()
        val localFiles = vaultDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) } ?: emptyArray()

        val html = buildString {
            append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>MazZe Tools - Fire TV & Android Transfer Hub</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                        body { background: #080A11; color: #F8FAFC; padding: 24px 16px; max-width: 900px; margin: 0 auto; }
                        .header { text-align: center; margin-bottom: 28px; padding-bottom: 20px; border-bottom: 1px solid #1E2536; }
                        .logo { font-size: 32px; font-weight: 800; color: #A855F7; letter-spacing: -0.5px; }
                        .logo span { color: #00F0FF; }
                        .tagline { color: #94A3B8; font-size: 14px; margin-top: 6px; }
                        .status-box { background: #0F1422; border: 1px solid #263354; border-radius: 12px; padding: 16px; margin-bottom: 24px; text-align: center; }
                        .status-box h3 { color: #00F0FF; font-size: 16px; margin-bottom: 4px; }
                        .card { background: #131A2C; border: 1px solid #263354; border-radius: 14px; padding: 18px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
                        .card-info { flex: 1; min-width: 220px; }
                        .card-title { font-size: 18px; font-weight: 700; color: #FFFFFF; }
                        .card-sub { font-size: 13px; color: #94A3B8; margin-top: 4px; }
                        .code-pill { background: #3B0764; color: #C084FC; border: 1px solid #A855F7; font-weight: bold; padding: 2px 8px; border-radius: 6px; font-size: 12px; display: inline-block; margin-right: 6px; }
                        .btn-dl { background: linear-gradient(135deg, #A855F7, #7E22CE); color: #FFFFFF; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: 800; font-size: 15px; display: inline-block; transition: opacity 0.2s; box-shadow: 0 4px 14px rgba(168, 85, 247, 0.4); }
                        .btn-dl:hover { opacity: 0.9; }
                        .btn-sec { background: #1E293B; color: #00F0FF; border: 1px solid #00F0FF; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; text-decoration: none; font-size: 13px; }
                        .section-title { font-size: 20px; font-weight: 700; margin: 24px 0 16px 0; color: #C084FC; }
                        .upload-box { background: #0F1422; border: 2px dashed #263354; border-radius: 12px; padding: 24px; text-align: center; margin-top: 32px; }
                        .tip { background: #181E30; border: 1px solid #00F0FF; color: #E0F2FE; padding: 12px; border-radius: 8px; margin-bottom: 20px; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="logo">⚡ MAZZE <span>TOOLS</span></div>
                        <div class="tagline">Direct local Wi-Fi transfer to Firestick, Smart TV, and Android devices</div>
                    </div>
                    
                    <div class="tip">
                        📺 <strong>Firestick Tip:</strong> On your Fire TV, open the <strong>Downloader app</strong> or <strong>Silk Browser</strong> and navigate to this page URL to install APKs in 1 tap!
                    </div>

                    <div class="status-box">
                        <h3>Connected Senders & Storage</h3>
                        <p style="color: #9EA7C0; font-size: 14px; margin-top: 4px;">Ready to beam. Click Download on any app below.</p>
                    </div>

                    <div class="section-title">📦 Ready to Install on TV (${localFiles.size} APKs available)</div>
            """.trimIndent())

            if (localFiles.isEmpty()) {
                append("""
                    <div class="card" style="text-align: center; justify-content: center;">
                        <p style="color: #7E8B9B;">No APKs saved yet in Vault. Extract an app or download from the Catalog on your phone!</p>
                    </div>
                """)
            } else {
                localFiles.forEachIndexed { index, file ->
                    val sizeMb = String.format("%.1f MB", file.length() / (1024.0 * 1024.0))
                    val displayName = file.nameWithoutExtension.replace("_", " ")
                    val dlUrl = "/apk/$index"
                    append("""
                        <div class="card">
                            <div class="card-info">
                                <div class="card-title">$displayName</div>
                                <div class="card-sub"><span class="code-pill">APK</span> Size: $sizeMb &bull; Ready for Firestick / Android</div>
                            </div>
                            <div>
                                <a href="$dlUrl" class="btn-dl">⬇ Download APK</a>
                            </div>
                        </div>
                    """)
                }
            }

            append("""
                    <div class="upload-box">
                        <h3 style="color: #00E5FF; margin-bottom: 8px;">📤 Beam APK From This Browser to Device</h3>
                        <p style="color: #8392A7; font-size: 13px; margin-bottom: 16px;">Have an APK file on your PC or tablet? Select it to send directly to APK Drop:</p>
                        <form method="POST" action="/upload" enctype="multipart/form-data">
                            <input type="file" name="apkfile" accept=".apk" style="color: #FFF; margin-bottom: 12px; font-size: 14px;"><br>
                            <input type="submit" value="Upload & Store in Vault" class="btn-sec">
                        </form>
                    </div>
                </body>
                </html>
            """.trimIndent())
        }

        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private suspend fun serveApkById(id: Long, output: BufferedOutputStream) {
        val vaultDir = apkRepository.getVaultDirectory()
        val localFiles = vaultDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) } ?: emptyArray()

        val file = if (id >= 0 && id < localFiles.size) {
            localFiles[id.toInt()]
        } else {
            // Also try matching by DB item
            val dbItem = apkRepository.getById(id)
            dbItem?.localFilePath?.let { File(it) }
        }

        if (file != null && file.exists()) {
            streamFile(file, output)
        } else {
            send404(output)
        }
    }

    private suspend fun serveApkByCode(code: String, output: BufferedOutputStream) {
        val item = apkRepository.getByQuickCode(code)
        val file = item?.localFilePath?.let { File(it) }

        if (file != null && file.exists()) {
            streamFile(file, output)
        } else {
            val vaultDir = apkRepository.getVaultDirectory()
            val fallback = vaultDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) }?.firstOrNull()
            if (fallback != null) {
                streamFile(fallback, output)
            } else {
                send404(output)
            }
        }
    }

    private suspend fun streamFile(file: File, output: BufferedOutputStream) = withContext(Dispatchers.IO) {
        val fileLength = file.length()
        val safeName = file.name.replace("\"", "")

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/vnd.android.package-archive\r\n" +
                "Content-Length: $fileLength\r\n" +
                "Content-Disposition: attachment; filename=\"$safeName\"\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Connection: close\r\n\r\n"

        output.write(header.toByteArray())
        output.flush()

        val buffer = ByteArray(64 * 1024)
        var totalBytesRead = 0L

        FileInputStream(file).use { fis ->
            val bis = BufferedInputStream(fis)
            var bytesRead: Int
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }
            output.flush()
        }

        _status.value = _status.value.copy(
            totalTransferredBytes = _status.value.totalTransferredBytes + totalBytesRead,
            lastEvent = "Transferred ${file.name} (${String.format("%.1f MB", totalBytesRead / (1024.0 * 1024.0))})"
        )
    }

    private suspend fun serveApiList(output: BufferedOutputStream) {
        val vaultDir = apkRepository.getVaultDirectory()
        val localFiles = vaultDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) } ?: emptyArray()

        val json = buildString {
            append("[")
            localFiles.forEachIndexed { idx, file ->
                val name = file.nameWithoutExtension.replace("\"", "\\\"")
                append("""{"id":$idx,"name":"$name","size":${file.length()},"downloadUrl":"/apk/$idx"}""")
                if (idx < localFiles.size - 1) append(",")
            }
            append("]")
        }

        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(bytes)
        output.flush()
    }

    private suspend fun handleUpload(
        headers: Map<String, String>,
        rawInput: java.io.InputStream,
        output: BufferedOutputStream
    ) = withContext(Dispatchers.IO) {
        val contentType = headers["content-type"] ?: ""
        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

        if (contentLength > 0) {
            val vaultDir = apkRepository.getVaultDirectory()
            val targetFile = File(vaultDir, "uploaded_app_${System.currentTimeMillis()}.apk")

            // Simple stream copy (saving raw stream or payload)
            FileOutputStream(targetFile).use { fos ->
                val buffer = ByteArray(32 * 1024)
                var remaining = contentLength
                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = rawInput.read(buffer, 0, toRead)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    remaining -= read
                }
            }

            _status.value = _status.value.copy(
                lastEvent = "Uploaded APK saved to Vault: ${targetFile.name}"
            )

            val redirectHtml = "<html><body><h2>Upload Successful!</h2><p>Saved to MazZe Tools Vault.</p><a href='/'>Return to Hub</a></body></html>"
            val respBytes = redirectHtml.toByteArray()
            val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: ${respBytes.size}\r\nConnection: close\r\n\r\n"
            output.write(header.toByteArray())
            output.write(respBytes)
            output.flush()
        } else {
            send404(output)
        }
    }

    private fun send404(output: BufferedOutputStream) {
        val notFound = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: 9\r\nConnection: close\r\n\r\nNot Found"
        output.write(notFound.toByteArray())
        output.flush()
    }
}
