# R8 rules for the release build. Anything found by name at run time (reflection) has to be kept, or the minifier
# renames or removes it and the app breaks only in release.

# Keep line numbers in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson: it reads generic types and annotations at run time. The weather answer is read with Gson's JsonParser (a tree,
# no reflection into our classes), but the weather models are kept too, so a later switch to fromJson cannot break.
-keepattributes Signature,*Annotation*
-keep class com.auradock.weather.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room: the generated database and the table classes are looked up and filled by name.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class com.auradock.tasks.TaskEntity { *; }

# Hilt / Dagger: generated components and view models are looked up by name. Hilt ships most of its own rules; these
# cover the entry points and the classes the manifest starts (the screensaver, receivers, listener service).
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Release logging: debug and verbose calls are removed. Info, warning and error stay; DockDream and DockWatcher log at info,
# because the A55 checklist reads them with `adb logcat -s DockDream DockWatcher`. None of the messages carry personal content.
-assumenosideeffects class android.util.Log {
    public static int v(...) return 0;
    public static int d(...) return 0;
}
