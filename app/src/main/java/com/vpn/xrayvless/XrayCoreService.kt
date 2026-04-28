package com.vpn.xrayvless

import android.content.Context
import kotlinx.coroutines.*
import java.io.File

class XrayCoreService(private val context: Context) {

    companion object { const val SOCKS_PORT = 10808 }

    private var isRunning = false
    private var job: Job? = null
    private var process: Process? = null

    fun start(config: VlessConfig): Boolean {
        if (isRunning) return false
        try {
            val configDir = File(context.filesDir, "xray")
            configDir.mkdirs()
            
            val json = """{"log":{"loglevel":"warn"},"inbounds":[{"tag":"socks-in","port":$SOCKS_PORT,"listen":"127.0.0.1","protocol":"socks","settings":{"auth":"noauth","udp":true}}],"outbounds":[{"tag":"proxy","protocol":"vless","settings":{"vnext":[{"address":"${config.server}","port":${config.port},"users":[{"id":"${config.uuid}","encryption":"${config.encryption}"}]}]},"streamSettings":{"network":"${config.type}","security":"${config.security}","tlsSettings":{"serverName":"${config.sni.ifEmpty { config.server }}","allowInsecure":${config.insecure || config.allowInsecure}},"xhttpSettings":{"host":"${config.host}","mode":"${config.mode}","path":"${config.path}"}}},{"tag":"direct","protocol":"freedom"}],"routing":{"rules":[{"type":"field","inboundTag":["socks-in"],"outboundTag":"proxy"}]}}"""
            
            File(configDir, "config.json").writeText(json)
            
            val xrayPath = File(context.applicationInfo.nativeLibraryDir, "libxray.so").absolutePath
            
            job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    process = ProcessBuilder(xrayPath, "run", "-config", File(configDir, "config.json").absolutePath)
                        .directory(configDir).redirectErrorStream(true).start()
                    isRunning = true
                    LogManager.addLog("✅ Xray SOCKS5:${SOCKS_PORT}")
                    process?.waitFor()
                    isRunning = false
                } catch (e: Exception) {
                    LogManager.addLog("❌ Xray: ${e.message}")
                    isRunning = false
                }
            }
            Thread.sleep(2000)
            return isRunning
        } catch (e: Exception) {
            LogManager.addLog("❌ ${e.message}")
            return false
        }
    }

    fun stop() {
        isRunning = false
        process?.destroy()
        job?.cancel()
    }
}
