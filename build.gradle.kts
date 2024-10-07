plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "7.4.2" apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// Removed duplicate plugin declaration

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.14")
    }
}

