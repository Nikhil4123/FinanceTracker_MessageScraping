plugins {
    alias(libs.plugins.android.application)  // Keep this line
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.finandetails"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.finandetails"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Use Firebase BoM to manage Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    // Firebase libraries (no need to specify versions explicitly)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Third-party libraries
    implementation("com.github.MrNouri:DynamicSizes:1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
}


apply(plugin = "com.google.gms.google-services")
