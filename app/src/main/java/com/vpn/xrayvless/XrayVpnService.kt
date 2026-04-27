package com.vpn.xrayvless

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.vpn.xrayvless.tun2socks.Tun2Socks
import java.net.InetSocketAddress
import java.net.Socket

class XrayVpnService : VpnService() {

    inner class LocalBinder : Binder() {
        fun getService(): XrayVpnService = this@XrayVpnService
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xray: XrayCoreService? = null
    private var tun2socks: Tun2Socks? = null

    override fun onBind(i: Intent?) = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        LogManager.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel("x", "VPN", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        try {
            val n = NotificationCompat.Builder(this, "x")
                .setContentTitle("XRAY VPN").setContentText("Conectando...")
                .setSmallIcon(R.drawable.ic_chave_vpn).setOngoing(true).build()
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(1, n)

            val c = Gson().fromJson(i?.getStringExtra("vless_config"), VlessConfig::class.java)

            // Iniciar Xray (SOCKS5)
            xray = XrayCoreService(this)
            if (!xray!!.start(c)) { stopSelf(); return START_NOT_STICKY }

            // Criar VPN
            val builder = Builder()
                .setSession(c.remark)
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)
            try { builder.addDisallowedApplication(packageName) } catch (e: Exception) {}

            vpnInterface = builder.establish()
            if (vpnInterface == null) { stopSelf(); return START_NOT_STICKY }

            // Proteger SOCKS
            try {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", XrayCoreService.SOCKS_PORT), 5000)
                protect(s)
                s.close()
                LogManager.addLog("?? SOCKS protegido")
            } catch (e: Exception) {}

            // USAR Tun2Socks do NexusVPN (que funciona!)
            LogManager.addLog("Iniciando Tun2Socks...")
            tun2socks = Tun2Socks(
                this,
                vpnInterface,
                1500,
                "10.0.0.2",
                "255.255.255.0",
                "127.0.0.1:${XrayCoreService.SOCKS_PORT}",
                "127.0.0.1:7300",
                "1.1.1.1",
                false
            )
            tun2socks!!.start()
            LogManager.addLog("✅ VPN + Tun2Socks ativos!")

        } catch (e: Exception) {
            LogManager.addLog("❌ ${e.message}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tun2socks?.interrupt()
        tun2socks = null
        xray?.stop()
        try { vpnInterface?.close() } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
