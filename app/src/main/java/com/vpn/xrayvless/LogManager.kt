package com.vpn.xrayvless
import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private var logFile: File? = null
    private val listeners = mutableListOf<(String) -> Unit>()
    
    fun init(context: Context) {
        if (logFile == null) {
            logFile = File(context.filesDir, "xray_logs.txt")
            addLog("=== APP INICIADO ===")
        }
    }
    
    fun addLog(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$ts] $msg"
        android.util.Log.d("XRAY", line)
        try { logFile?.appendText(line + "\n") } catch (e: Exception) {}
        listeners.forEach { it(line) }
    }
    
    fun getLogs() = try { logFile?.readText() ?: "" } catch (e: Exception) { "" }
    fun addListener(l: (String) -> Unit) { listeners.add(l) }
}
