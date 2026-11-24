package io.github.davshn.narsil

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// The alerts come again once the phone starts, or once the app is updated, without opening it.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AlertService.start(context)
        }
    }
}
