plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

        // Base URL no BuildConfig
        buildConfigField("String", "API_BASE_URL", "\"https://painel.tolon.com.br/\"")

        // Fallback opcional
        buildConfigField("String", "DEFAULT_EMPRESA", "\"mit\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_EMPRESA", "\"mit\"") // <-- coloca o CÓDIGO CERTO AQUI
            buildConfigField("String", "API_USUARIO", "\"\"")
            buildConfigField("String", "API_TOKEN",   "\"\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_EMPRESA", "\"mit\"") // idem (ou configura via CI)
            buildConfigField("String", "API_USUARIO", "\"\"")
            buildConfigField("String", "API_TOKEN",   "\"\"")
        }
    }


    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(platform("io.coil-kt:coil-bom:2.6.0"))
    implementation("io.coil-kt:coil-compose")

    implementation("androidx.compose.material:material-icons-extended")
}
