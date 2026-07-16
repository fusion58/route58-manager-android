/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.manager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebViewFragment
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
        updateEventId(intent)
        if (savedInstanceState == null) {
            initContent()
        }
    }

    private fun initContent() {
        // Route58 apunta SIEMPRE a su propio servidor (BuildConfig.SERVER_URL). El usuario no
        // elige servidor ni ve pantalla de configuración: va directo al login (#136).
        fragmentManager.beginTransaction().add(android.R.id.content, MainFragment()).commit()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        updateEventId(intent)
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainFragment.EVENT_EVENT))
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val fragment = fragmentManager.findFragmentById(android.R.id.content)
        fragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        val fragment = fragmentManager.findFragmentById(android.R.id.content) as? WebViewFragment
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
