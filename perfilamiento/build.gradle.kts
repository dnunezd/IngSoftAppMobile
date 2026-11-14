// Recorridos de perfilamiento (docs/performance): manejan la app "perf" con UI Automator desde un
// proceso aparte, para que el propio recorrido no altere lo que mide el Profiler.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.vinilos.perfilamiento"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Mismo nombre que el tipo de compilación de :app al que apunta.
        create("perf") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    targetProjectPath = ":app"
    // El APK de recorridos se instrumenta a sí mismo: no se carga dentro del proceso de la app.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

androidComponents {
    // Solo existe la variante "perf": los recorridos no tienen sentido contra debug ni release.
    beforeVariants(selector().all()) { variante ->
        variante.enable = variante.buildType == "perf"
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
}
