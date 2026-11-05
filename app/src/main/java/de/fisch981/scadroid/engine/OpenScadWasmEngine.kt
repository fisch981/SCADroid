package de.fisch981.scadroid.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.StringBuilder

class OpenScadWasmEngine(private val context: Context) {

    private val tag = "OpenScadWasm"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    var isReady = false
        private set

    private val readyDeferred = CompletableDeferred<Boolean>()
    private var currentDeferred: CompletableDeferred<String>? = null
    private val chunkBuffer = StringBuilder()
    var lastError: String? = null
        private set

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            val wv = WebView(context)
            wv.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                domStorageEnabled = true
            }

            wv.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(tag, "[JS] ${consoleMessage?.messageLevel()}: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                    return true
                }
            }

            wv.webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    val msg = "WebView Error: ${error?.description}"
                    Log.e(tag, msg)
                    lastError = msg
                }
            }

            wv.addJavascriptInterface(JsBridge(), "AndroidBridge")
            wv.loadUrl("file:///android_asset/openscad/engine.html")
            webView = wv
        } catch (e: Exception) {
            val msg = "WebView Init Exception: ${e.message}"
            Log.e(tag, msg, e)
            lastError = msg
            readyDeferred.complete(false)
        }
    }

    suspend fun render(scadCode: String, timeoutMs: Long = 180000): String {
        // Warte bis die WASM-Engine betriebsbereit ist
        if (!isReady) {
            val ready = withTimeoutOrNull(25000) {
                readyDeferred.await()
            }
            if (ready != true) {
                val err = lastError ?: "WASM-Engine konnte nicht innerhalb von 25 Sekunden initialisiert werden."
                throw RuntimeException(err)
            }
        }

        val deferred = CompletableDeferred<String>()
        currentDeferred = deferred

        mainHandler.post {
            try {
                val b64 = Base64.encodeToString(scadCode.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                webView?.evaluateJavascript("window.renderScadBase64('$b64');", null)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }

        val result = withTimeoutOrNull(timeoutMs) {
            deferred.await()
        } ?: throw RuntimeException("Zeitüberschreitung bei der OpenSCAD-Berechnung ($timeoutMs ms)")

        return result
    }

    private inner class JsBridge {
        @JavascriptInterface
        fun onEngineReady() {
            Log.i(tag, "OpenSCAD WASM Engine ist bereit.")
            isReady = true
            readyDeferred.complete(true)
        }

        @JavascriptInterface
        fun onRenderSuccess(stlData: String) {
            currentDeferred?.complete(stlData)
            currentDeferred = null
        }

        @JavascriptInterface
        fun onRenderChunkStart(totalLen: Int) {
            chunkBuffer.setLength(0)
        }

        @JavascriptInterface
        fun onRenderChunk(chunk: String) {
            chunkBuffer.append(chunk)
        }

        @JavascriptInterface
        fun onRenderChunkEnd() {
            val fullStl = chunkBuffer.toString()
            chunkBuffer.setLength(0)
            currentDeferred?.complete(fullStl)
            currentDeferred = null
        }

        @JavascriptInterface
        fun onError(errorMsg: String) {
            Log.e(tag, "OpenSCAD Engine Fehler: $errorMsg")
            lastError = errorMsg
            currentDeferred?.completeExceptionally(RuntimeException(errorMsg))
            currentDeferred = null
        }
    }
}
