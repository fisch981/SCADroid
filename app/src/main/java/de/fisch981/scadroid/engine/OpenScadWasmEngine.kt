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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.lang.StringBuilder

class OpenScadWasmEngine(
    private val context: Context,
    var onConsoleLog: ((String) -> Unit)? = null
) {

    private val tag = "OpenScadWasm"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val renderMutex = Mutex()
    private var webView: WebView? = null
    var isReady = false
        private set

    private val readyDeferred = CompletableDeferred<Boolean>()
    private var currentDeferred: CompletableDeferred<String>? = null
    private val chunkBuffer = StringBuilder()
    private var cachedFontBase64: String? = null
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
                    val msg = consoleMessage?.message() ?: ""
                    Log.d(tag, "[JS] ${consoleMessage?.messageLevel()}: $msg (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                    if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR && !isReady) {
                        lastError = "JS Error: $msg (Zeile ${consoleMessage.lineNumber()})"
                        if (!readyDeferred.isCompleted) {
                            readyDeferred.complete(false)
                        }
                    }
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
            if (!readyDeferred.isCompleted) {
                readyDeferred.complete(false)
            }
        }
    }

    suspend fun renderBinaryStl(scadCode: String, timeoutMs: Long = 180000): ByteArray {
        val b64 = render(scadCode, timeoutMs)
        if (b64.isBlank()) {
            return ByteArray(0)
        }
        return Base64.decode(b64, Base64.DEFAULT)
    }

    suspend fun render(scadCode: String, timeoutMs: Long = 180000): String = renderMutex.withLock {
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

        result
    }

    private inner class JsBridge {
        @JavascriptInterface
        fun onEngineReady() {
            Log.i(tag, "OpenSCAD WASM Engine ist bereit.")
            isReady = true
            if (!readyDeferred.isCompleted) {
                readyDeferred.complete(true)
            }
        }

        @JavascriptInterface
        fun onConsoleLog(msg: String) {
            onConsoleLog?.invoke(msg)
        }

        @JavascriptInterface
        fun getFontBase64(): String {
            cachedFontBase64?.let { return it }
            return try {
                val bytes = context.assets.open("openscad/LiberationSans-Regular.ttf").use { it.readBytes() }
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                cachedFontBase64 = b64
                b64
            } catch (e: Exception) {
                Log.w(tag, "Standard-Schriftart konnte nicht aus Assets geladen werden, versuche System-Font: ${e.message}")
                try {
                    val sysFont = File("/system/fonts/Roboto-Regular.ttf")
                    if (sysFont.exists()) {
                        val b64 = Base64.encodeToString(sysFont.readBytes(), Base64.NO_WRAP)
                        cachedFontBase64 = b64
                        b64
                    } else ""
                } catch (_: Exception) {
                    ""
                }
            }
        }

        @JavascriptInterface
        fun getProjectFileBase64(fileName: String): String {
            return try {
                val cleanName = fileName.substringAfterLast('/').substringAfterLast('\\')
                val direct = File(context.filesDir, cleanName)
                val target = if (direct.exists()) {
                    direct
                } else {
                    context.filesDir.listFiles()?.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
                }
                if (target != null && target.exists() && target.isFile) {
                    Base64.encodeToString(target.readBytes(), Base64.NO_WRAP)
                } else {
                    ""
                }
            } catch (e: Exception) {
                Log.w(tag, "Fehler beim Laden der Projektdatei '$fileName': ${e.message}")
                ""
            }
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
            if (!isReady && !readyDeferred.isCompleted) {
                readyDeferred.complete(false)
            }
            currentDeferred?.completeExceptionally(RuntimeException(errorMsg))
            currentDeferred = null
        }
    }
}
