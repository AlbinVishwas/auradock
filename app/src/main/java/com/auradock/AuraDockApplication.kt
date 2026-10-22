package com.auradock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. @HiltAndroidApp triggers Hilt's code generation. */
@HiltAndroidApp
class AuraDockApplication : Application()
