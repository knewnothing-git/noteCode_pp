package com.example.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TermuxResultReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TermuxResultReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "Received Termux execution result broadcast: ${intent.action}")
        try {
            TermuxManager.handleResult(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Termux result: ${e.message}", e)
        }
    }
}
