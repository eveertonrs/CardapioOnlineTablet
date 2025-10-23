package com.helptech.abraham.integracao

import android.content.Context
import android.provider.Settings

fun getAndroidId(ctx: Context): String {
    return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "UNKNOWN_SN"
}
