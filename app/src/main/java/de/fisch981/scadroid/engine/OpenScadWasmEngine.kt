package de.fisch981.scadroid.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.StringBuilder

class OpenScadWasmEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    var isReady = false
        private set

    private val readyDeferred = CompletableDeferred<Boolean>()
    private var currentDeferred: CompletableDeferred<String>? = null
    private val chunkBuffer = StringBuilder()

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

            wv.addJavascriptInterface(JsBridge(), "AndroidBridge")

            wv.webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                }
            }

            wv.loadUrl("file:///android_asset/openscad/engine.html")
            webView = wv
        } catch (e: Exception) {
            readyDeferred.complete(false)
        }
    }

    suspend fun render(scadCode: String, timeoutMs: Long = 60000): String {
        // Warte bis die WASM-Engine betriebsbereit ist
        if (!isReady) {
            withTimeoutOrNull(15000) {
                readyDeferred.await()
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
            currentDeferred?.completeExceptionally(RuntimeException(errorMsg))
            currentDeferred = null
        }
    }
}
