package com.auradock

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** The one place the privacy policy's address is written. The Settings row and the permission sheet both open it. */
const val PRIVACY_POLICY_URL = "https://albinvishwas.github.io/auradock/privacy/"

/** Opens the privacy policy in the browser. Does nothing if the phone has no browser. */
fun openPrivacyPolicy(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
