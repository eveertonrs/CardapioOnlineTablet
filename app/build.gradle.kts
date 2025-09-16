plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Kotlin 2.x + Compose
    alias(libs.plugins.kotlin.compose)
    // se não tiver o alias acima no version catalog, use:
    // id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.helptech.abraham"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.helptech.abraham"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "API_BASE_URL", "\"https://painel.tolon.com.br/\"")
        buildConfigField("String", "API_EMPRESA", "\"mit\"")
        buildConfigField("String", "API_USUARIO", "\"marchioreit\"")
        buildConfigField("String", "API_TOKEN", "\"1697425409689f9ccda794d9.81360355\"")
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Com Kotlin 2.x não precisa definir composeOptions
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Kotlin 17 (toolchain)
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Retrofit / OkHttp / Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (use BOM e somente coil-compose)
    implementation(platform("io.coil-kt:coil-bom:2.6.0"))
    implementation("io.coil-kt:coil-compose")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material:material-icons-extended")


    // se precisar: implementation("io.coil-kt:coil-gif") / implementation("io.coil-kt:coil-svg")
}
