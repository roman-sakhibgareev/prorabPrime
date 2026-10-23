import com.android.build.api.variant.BuildConfigField
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("prorab.quality")
}

// versionCode is derived from the one version in gradle.properties, so the two cannot drift.
// Minor and patch are allowed 0-99 each.
val (versionMajor, versionMinor, versionPatch) = providers.gradleProperty("prorab.version").get()
    .split('.')
    .map(String::toInt)

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

// The server behind Caddy (Caddyfile); what a store or customer build talks to.
val publicServer = "https://prorabprime.duckdns.org"

// Each flavor carries the server the app talks to until the user changes it in the settings. Values come
// from local.properties (gitignored: a real token never enters the repository); a Gradle property wins.
// Read lazily, so a flavor that is not being built never asks for its values.
fun buildConfigString(key: String, default: String = ""): Provider<String> = provider {
    val value = providers.gradleProperty(key).orNull ?: localProperties.getProperty(key) ?: default
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

// Every flavor starts the app on a server of its own until the user changes it in the settings:
// - rustore: the public server, no token (the user types theirs in the settings);
// - dev: the owner's local builds, the emulator's host machine unless local.properties says otherwise;
// - client: the public server and the customer's token, so the APK opens ready to work.
fun flavorServerUrl(flavor: String) = when (flavor) {
    "dev" -> buildConfigString("prorab.dev.serverUrl", "http://10.0.2.2:8080")
    else -> buildConfigString("prorab.$flavor.serverUrl", publicServer)
}

fun flavorApiToken(flavor: String) = when (flavor) {
    "rustore" -> provider { "\"\"" }
    else -> buildConfigString("prorab.$flavor.apiToken")
}

// The AppMetrica API key of each flavor, from a Gradle property, local.properties or PRORAB_<FLAVOR>_APPMETRICA_KEY.
// Empty means AppMetrica stays off. A debug build never gets one, so a build from the IDE cannot reach the data of
// the store's app; `dev` has a key of its own to try the setup in a release build.
fun appMetricaKey(flavor: String, buildType: String): Provider<String> {
    if (buildType == "debug") return provider { "\"\"" }
    val env = providers.environmentVariable("PRORAB_${flavor.uppercase()}_APPMETRICA_KEY")
    return buildConfigString("prorab.$flavor.appmetricaKey", env.getOrElse(""))
}

// An APK for the customer without its token would open empty; only building that APK needs the token, so
// `verify` and CI, which compile every variant, are not stopped by it.
tasks.matching { it.name == "assembleClientRelease" || it.name == "bundleClientRelease" }.configureEach {
    doFirst {
        check(!flavorApiToken("client").get().trim('"').isEmpty()) {
            "prorab.client.apiToken is not set: put it in local.properties"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val flavor = variant.flavorName.orEmpty()
        variant.buildConfigFields?.put(
            "DEFAULT_SERVER_URL",
            flavorServerUrl(flavor).map { BuildConfigField("String", it, null) },
        )
        variant.buildConfigFields?.put(
            "DEFAULT_API_TOKEN",
            flavorApiToken(flavor).map { BuildConfigField("String", it, null) },
        )
        variant.buildConfigFields?.put(
            "APPMETRICA_API_KEY",
            appMetricaKey(flavor, variant.buildType.orEmpty()).map { BuildConfigField("String", it, null) },
        )
    }
}

// The release signing key, from keystore.properties (gitignored) or, on a build machine, from the
// environment. Without either the release build is simply left unsigned, so `verify` needs no key.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(key: String, variable: String): String? =
    keystoreProperties.getProperty(key) ?: providers.environmentVariable(variable).orNull

val releaseStoreFile = signingValue("storeFile", "PRORAB_STORE_FILE")

android {
    namespace = "ru.prorabprime"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.prorabprime"
        minSdk = 26
        targetSdk = 37
        versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("rustore") { dimension = "distribution" }
        create("dev") { dimension = "distribution" }
        create("client") { dimension = "distribution" }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "PRORAB_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "PRORAB_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "PRORAB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// :app configures Compose directly rather than through a convention plugin (those are for
// libraries), so the shared stability config is pointed at by hand.
composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("config/compose-stability.conf"))
}

dependencies {
    // The whole UI and the Koin module list; this module is only the Android entry point.
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    // FileProvider, for the pictures handed to the share sheet.
    implementation(libs.androidx.core.ktx)
    // Coil's singleton loader is built here, over the app's HttpClient (ADR-0003, ADR-0008).
    implementation(platform(libs.coil.bom))
    implementation(libs.coil)
    implementation(libs.coil.network.ktor3)
    // Crash reports and analytics; only :app knows the SDK (ADR-0029).
    implementation(libs.appmetrica)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
