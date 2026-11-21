/*
 * Route58 Manager para Android (Fusion58). Basado en software libre.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")
package com.fusion58.route58.manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.webkit.WebViewFragment
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    var pendingEventId: Long? = null

    // Splash de arranque (#568): se sostiene hasta que el login termina de cargar en la
    // WebView (MainFragment lo pone en true desde onPageFinished), con tope de seguridad.
    @Volatile
    var contentReady = false

    private lateinit var sinConexion: View

    // Si vuelve la red mientras se ve la pantalla nativa, se reintenta solo.
    private val monitorRed = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread {
                if (::sinConexion.isInitialized && sinConexion.visibility == View.VISIBLE) {
                    reintentar()
                }
            }
        }
    }

    private fun updateEventId(intent: Intent?) {
        intent?.getStringExtra("eventId")?.let { pendingEventId = it.toLongOrNull() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Mantener el splash hasta que la WebView cargue el login (o venza el tope).
        splashScreen.setKeepOnScreenCondition { !contentReady }
        Handler(Looper.getMainLooper()).postDelayed({ contentReady = true }, SPLASH_MAX_MS)
        setContentView(R.layout.activity_main)

        sinConexion = findViewById(R.id.sin_conexion)
        findViewById<Button>(R.id.boton_reintentar).setOnClickListener { reintentar() }
        findViewById<Button>(R.id.boton_ajustes_red).setOnClickListener { abrirAjustesRed() }
        registrarMonitorRed()

        updateEventId(intent)
        if (savedInstanceState == null) {
            initContent()
        }
    }

    private fun initContent() {
        // Route58 apunta SIEMPRE a su propio servidor (BuildConfig.SERVER_URL). El usuario no
        // elige servidor ni ve pantalla de configuración: va directo al login (#136).
        fragmentManager.beginTransaction().add(R.id.contenido, MainFragment()).commit()
    }

    fun mostrarSinConexion() {
        runOnUiThread {
            sinConexion.visibility = View.VISIBLE
            sinConexion.bringToFront()
        }
    }

    fun ocultarSinConexion() {
        runOnUiThread { sinConexion.visibility = View.GONE }
    }

    private fun reintentar() {
        ocultarSinConexion()
        (fragmentManager.findFragmentById(R.id.contenido) as? MainFragment)?.recargar()
    }

    private fun abrirAjustesRed() {
        try {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                // Sin ajustes disponibles: el botón Reintentar sigue funcionando.
            }
        }
    }

    private fun registrarMonitorRed() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, monitorRed)
        } catch (e: Exception) {
            // Si el sistema no lo permite, la pantalla igual se puede reintentar a mano.
        }
    }

    private fun quitarMonitorRed() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(monitorRed)
        } catch (e: Exception) {
            // Nada que liberar.
        }
    }

    override fun onDestroy() {
        quitarMonitorRed()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        updateEventId(intent)
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainFragment.EVENT_EVENT))
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val fragment = fragmentManager.findFragmentById(R.id.contenido)
        fragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        val fragment = fragmentManager.findFragmentById(R.id.contenido) as? WebViewFragment
        if (fragment?.webView?.canGoBack() == true) {
            fragment.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val PREFERENCE_URL = "url"

        // Tope de seguridad del splash: si el login tarda o no hay red, no se queda colgado.
        private const val SPLASH_MAX_MS = 6000L
    }
}
