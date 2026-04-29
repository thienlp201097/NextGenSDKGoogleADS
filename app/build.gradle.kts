import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}
val packageName = "com.lib.dktechads"

android {
    namespace = "com.lib.dktechads"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lib.dktechads"
        minSdk = 26
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures{
        viewBinding = true
    }
}

tasks.register("generateRemoteConfig") {
    group = "codegen"

    val xmlFile = file("src/main/res/xml/remote_config_defaults.xml")
    val outputDir = file("build/generated/source/remoteConfig/")
    val outputFile = File(outputDir, "RemoteConfig.kt")

    doLast {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")

        val builder = StringBuilder()
        builder.appendLine("package $packageName")
        builder.appendLine()
        builder.appendLine("object RemoteConfig {")

        fun toScreamingSnakeCase(input: String): String {
            return input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .replace(Regex("[^A-Za-z0-9]"), "_")
                .uppercase()
        }

        for (i in 0 until entries.length) {
            val entry = entries.item(i)
            val key = entry.childNodes.item(1).textContent.trim()
            val constKey = toScreamingSnakeCase(key)
            builder.appendLine("    const val $constKey = \"$key\"")
        }

        builder.appendLine("}")
        val newContent = builder.toString()
        if (!outputFile.exists() || outputFile.readText() != newContent) {
            outputFile.parentFile.mkdirs()
            outputFile.writeText(newContent)
        }
    }
}

tasks.named("preBuild") {
    dependsOn("generateRemoteConfig")
}

android.sourceSets.getByName("main").kotlin.srcDir("build/generated/source/remoteConfig/")


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project (":dktlibrary"))
    implementation("com.applovin:applovin-sdk:13.5.0")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.0.0")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
}