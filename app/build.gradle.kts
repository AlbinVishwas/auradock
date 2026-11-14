import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
}

// Release signing details live in keystore.properties (gitignored, never committed). Without it the release build is
// still produced, just unsigned, so anyone cloning the repo can build.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Sentry settings live OUTSIDE the repo, in ~/.auradock-signing/sentry.properties (dsn, auth.token, org, project). Nothing from that file is ever
// committed. Without it the build still succeeds, with crash reporting off and no mapping upload. The auth token is used only by the Gradle plugin
// to upload the R8 mapping; it is never put into the app.
val sentryProperties = Properties().apply {
    val file = File(System.getProperty("user.home"), ".auradock-signing/sentry.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val sentryDsn: String = sentryProperties.getProperty("dsn", "").trim()
val sentryAuthToken: String = sentryProperties.getProperty("auth.token", "").trim()
val sentryOrg: String = sentryProperties.getProperty("org", "").trim()
val sentryProject: String = sentryProperties.getProperty("project", "").trim()
// PostHog settings also live OUTSIDE the repo, in ~/.auradock-signing/posthog.properties: `key` or `api.key` (project API key), `host` (for example
// https://eu.i.posthog.com) and optionally `dev.key` (a separate "dev" project used only by debug builds that turn analytics on). Without the file the
// build succeeds with analytics off. The key is a public ingest key, like Sentry's DSN, so it is compiled into the app.
val posthogProperties = Properties().apply {
    val file = File(System.getProperty("user.home"), ".auradock-signing/posthog.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val posthogKey: String = (posthogProperties.getProperty("key") ?: posthogProperties.getProperty("api.key") ?: "").trim()
val posthogHost: String = posthogProperties.getProperty("host", "https://us.i.posthog.com").trim()
val posthogDevKey: String = posthogProperties.getProperty("dev.key", "").trim()
val sentryCanUpload = sentryAuthToken.isNotEmpty() && sentryOrg.isNotEmpty() && sentryProject.isNotEmpty()

// A release build with a missing key would ship with crash reports or analytics silently off, so say so loudly. Debug builds (and CI, which builds
// debug only and has no key files) stay quiet. The warning never fails the build.
gradle.taskGraph.whenReady {
    val releaseBuild = allTasks.any { it.project == project && Regex("^(assemble|bundle|package)\\w*Release$").matches(it.name) }
    if (releaseBuild) {
        val missing = buildList {
            if (sentryDsn.isEmpty()) add("Sentry DSN (`dsn` in ~/.auradock-signing/sentry.properties): crash reports will be OFF")
            if (posthogKey.isEmpty()) add("PostHog key (`key` or `api.key` in ~/.auradock-signing/posthog.properties): analytics will be OFF")
        }
        missing.forEach { logger.warn("WARNING: this release build has no $it") }
    }
}

android {
    namespace  = "com.auradock"
    compileSdk = 36

    defaultConfig {
        applicationId          = "com.auradock"
        minSdk                 = 26
        targetSdk              = 36
        versionCode            = 13
        versionName            = "0.3.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The DSN is a public identifier (it ships in every app that uses Sentry). It is empty when sentry.properties is missing: then Sentry stays off.
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
        buildConfigField("String", "POSTHOG_KEY", "\"$posthogKey\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$posthogHost\"")
        buildConfigField("String", "POSTHOG_DEV_KEY", "\"$posthogDevKey\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true // Robolectric tests can read the app's resources and manifest
    }

    // The exported Room schemas are test assets, so MigrationTestHelper can open a database as an older version.
    sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
}

// The Sentry Gradle plugin: uploads the R8 mapping on release builds (so crash stack traces are readable) and does nothing in debug builds.
sentry {
    org.set(sentryOrg)
    projectName.set(sentryProject)
    if (sentryAuthToken.isNotEmpty()) authToken.set(sentryAuthToken)
    includeProguardMapping.set(sentryCanUpload)
    autoUploadProguardMapping.set(sentryCanUpload)
    uploadNativeSymbols.set(false)
    includeNativeSources.set(false)
    includeSourceContext.set(false)
    // No automatic instrumentation of the app's own code: the dock's traces are written by hand (see monitoring/), so nothing else is recorded.
    tracingInstrumentation { enabled.set(false) }
    autoInstallation { enabled.set(false) }
    ignoredBuildTypes.set(setOf("debug"))
    telemetry.set(false)
}

// Room writes the database schemas here (one JSON per version). They are committed, and migration tests read them (see docs/TESTING.md).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.sentry.android)
    implementation(libs.posthog.android)
    implementation(libs.lifecycle.process)
    implementation(libs.appcompat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room (tasks)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.gson)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.coroutines.test)
}
