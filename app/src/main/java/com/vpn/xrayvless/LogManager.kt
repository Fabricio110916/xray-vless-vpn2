package com.vpn.xrayvless

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object LogManager {
    private var logFile: File? = null
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun init(context: Context) {
        if (logFile == null) {
            logFile = File(context.filesDir, "xray_logs.txt")
            addLog("=== APP INICIADO ===")
            addLog("Hora: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
        }
    }

    @JvmStatic
    fun addLog(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$ts] $msg"
        android.util.Log.d("XRAY", line)
        try {
            logFile?.appendText(line + "\n")
        } catch (e: Exception) {
            android.util.Log.e("XRAY", "Erro ao escrever log: ${e.message}")
        }
        for (l in listeners) {
            try { l(line) } catch (e: Exception) {}
        }
    }

    fun getLogs(): String {
        return try {
            logFile?.readText() ?: "Sem logs"
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    fun addListener(l: (String) -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: (String) -> Unit) {
        listeners.remove(l)
    }
}
