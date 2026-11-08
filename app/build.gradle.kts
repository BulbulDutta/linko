import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.time.Duration

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.linko.wqvkmz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.storage)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.zxing.core)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.play.services.ads) {
    exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices-java")
  }
  implementation(libs.stream.webrtc.android)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "kspDebug"(libs.androidx.room.compiler)
  "kspRelease"(libs.androidx.room.compiler)
}

class KspAwtExceptionHandler(
  private val delegate: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler, java.io.Serializable {
  override fun uncaughtException(t: Thread, e: Throwable) {
    val msg = e.message ?: ""
    val isKspAwtNpe =
      t.name.startsWith("AWT-EventQueue") &&
        e is NullPointerException &&
        (msg.contains("ksp.com.intellij.openapi.application.ApplicationManager.getApplication()") ||
          e.stackTrace.any { it.className.startsWith("ksp.com.intellij.") })
    if (isKspAwtNpe) {
      return
    }
    if (delegate != null) {
      delegate.uncaughtException(t, e)
    } else if (e.javaClass.name != "java.lang.ThreadDeath") {
      System.err.print("Exception in thread \"${t.name}\" ")
      e.printStackTrace(System.err)
    }
  }
}

open class InstallKspAwtHandlerAction : org.gradle.api.Action<org.gradle.api.Task>, java.io.Serializable {
  override fun execute(t: org.gradle.api.Task) {
    install()
  }

  companion object {
    fun install() {
      System.setProperty("java.awt.headless", "true")
      val current = Thread.getDefaultUncaughtExceptionHandler()
      val handler =
        if (current != null && current.javaClass.name.contains("KspAwtExceptionHandler")) {
          current
        } else {
          KspAwtExceptionHandler(current).also {
            Thread.setDefaultUncaughtExceptionHandler(it)
          }
        }
      Thread.getAllStackTraces().keys
        .filter { it.name.startsWith("AWT-EventQueue") }
        .forEach { it.uncaughtExceptionHandler = handler }
    }
  }
}

InstallKspAwtHandlerAction.install()

tasks.matching { it.name.startsWith("ksp") }.configureEach {
  doFirst(InstallKspAwtHandlerAction())
  doLast(InstallKspAwtHandlerAction())
}

tasks.withType<Test> {
  failFast = true
  timeout.set(Duration.ofMinutes(2))
  testLogging {
    events("passed", "failed", "skipped")
  }
}


