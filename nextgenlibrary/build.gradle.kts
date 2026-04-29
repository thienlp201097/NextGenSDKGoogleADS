import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.ads.detech"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        buildConfig = true
        viewBinding = true
    }
}

// GitHub Packages returns 422 if this exact (groupId, artifactId, version) was already published — bump version or delete the package version on GitHub.
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.detech.ads"
            artifactId = "nextGenLib"
            version = (project.findProperty("nextgenlib.version") as String?) ?: "1.0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/thienlp201097/NextGenSDKGoogleADS")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.tenjin:android-sdk:1.17.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")

    // UI
    implementation("androidx.appcompat:appcompat:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    implementation("com.pnikosis:materialish-progress:1.7")
    implementation("com.facebook.shimmer:shimmer:0.5.0@aar")

    // Ads
//    implementation("com.google.android.gms:play-services-ads:25.2.0")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.0.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    // Other
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.airbnb.android:lottie:6.7.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-config")

    implementation("com.facebook.android:facebook-android-sdk:18.1.3")

    implementation ("com.reyun.solar.engine.oversea:solar-engine-core:1.3.1.2"){
        exclude(group = "com.huawei.hms", module = "ads-identifier")
        exclude(group = "com.hihonor.mcs", module = "ads-identifier")
    }

    implementation("com.github.tiktok:tiktok-business-android-sdk:1.5.0")
}
