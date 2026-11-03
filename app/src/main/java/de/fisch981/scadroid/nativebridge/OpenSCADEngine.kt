package de.fisch981.scadroid.nativebridge

import android.util.Log

object OpenSCADEngine {
    private const val TAG = "OpenSCADEngine"

    init {
        try {
            System.loadLibrary("scadroid")
            Log.i(TAG, "Native library 'scadroid' successfully loaded.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library 'scadroid'", e)
        }
    }

    /**
     * Ruft die Versions- und Feature-Informationen der C++ NDK Engine ab.
     */
    external fun getEngineVersion(): String
}
