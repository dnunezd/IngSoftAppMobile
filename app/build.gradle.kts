plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    jacoco
}

android {
    namespace = "com.example.vinilos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vinilos"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.90"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // El emulador ve el localhost del anfitrión (donde corre BackVynils) como 10.0.2.2.
        // En un dispositivo físico se sobrescribe con -PbaseUrl=http://localhost:3000/ y `adb reverse tcp:3000 tcp:3000`.
        val baseUrl = (project.findProperty("baseUrl") as String?) ?: "http://10.0.2.2:3000/"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ---------- Cobertura de las pruebas unitarias (JaCoCo) ----------

jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        // Necesario con pruebas unitarias de Android y JDK 17.
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Clases que no aportan al cálculo: código generado y pegamento de Compose.
val exclusionesCobertura = listOf(
    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*ComposableSingletons*.*", "**/*\$\$inlined\$*.*", "**/*Preview*.*"
)

val clasesCubiertas = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
    exclude(exclusionesCobertura)
}
val datosDeEjecucion = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Informe de cobertura (JaCoCo) de las pruebas unitarias."
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(clasesCubiertas)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosDeEjecucion)
}

// Compuerta de calidad: la lógica (datos y ViewModels) debe mantenerse cubierta por las pruebas
// unitarias. La UI de Compose se valida con las pruebas instrumentadas, que no entran en este cálculo.
val jacocoTestCoverageVerification = tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Falla si la cobertura de la lógica (data y viewmodel) baja de los mínimos."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            include("com/example/vinilos/data/**", "com/example/vinilos/viewmodel/**")
            exclude(exclusionesCobertura)
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosDeEjecucion)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(jacocoTestCoverageVerification)
}
