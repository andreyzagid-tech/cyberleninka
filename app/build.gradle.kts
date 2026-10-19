import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Ключи рекламы держим вне репозитория: ads.properties в корне проекта (см. ads.properties.example).
// Если файла нет — используются тестовые ключи Appodeal/AdMob, чтобы сборка работала.
val adsProps = Properties().apply {
    val f = rootProject.file("ads.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun adsProp(key: String, def: String): String =
    adsProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: def

val appodealTestKey = "d908f77a97ae0993514bc8edba7e776a36593c77e5f44994"
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"

// Ключ подписи держим вне репозитория: keystore.properties в корне (см. keystore.properties.example).
// Без него release подписывается debug-ключом, чтобы сборку можно было проверить локально.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "ru.cyberleninka.app"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["admobAppId"] = admobTestAppId
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.getProperty("storeFile") != null) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystoreProps.getProperty("storeFile") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    flavorDimensions += "section"

    productFlavors {
        create("medical") {
            dimension = "section"
            applicationId = "ru.cyberleninka.medical"
            buildConfigField("String", "SECTION_TITLE", "\"Медицинские науки\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"basic-medicine,clinical-medicine,health-sciences,health-biotechnology\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.medical", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.medical", admobTestAppId)
        }
        create("natural") {
            dimension = "section"
            applicationId = "ru.cyberleninka.natural"
            buildConfigField("String", "SECTION_TITLE", "\"Естественные и точные науки\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"mathematics,computer-and-information-sciences,physical-sciences,chemical-sciences," +
                    "earth-and-related-environmental-sciences,biological-sciences\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.natural", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.natural", admobTestAppId)
        }
        create("engineering") {
            dimension = "section"
            applicationId = "ru.cyberleninka.engineering"
            buildConfigField("String", "SECTION_TITLE", "\"Техника и технологии\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"civil-engineering,electrical-electronic-information-engineering,mechanical-engineering," +
                    "chemical-engineering,materials-engineering,medical-engineering,environmental-engineering," +
                    "environmental-biotechnology,industrial-biotechnology,nano-technology\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.engineering", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.engineering", admobTestAppId)
        }
        create("humanities") {
            dimension = "section"
            applicationId = "ru.cyberleninka.humanities"
            buildConfigField("String", "SECTION_TITLE", "\"Гуманитарные науки\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"history-and-archaeology,languages-and-literature,philosophy-ethics-and-religion," +
                    "arts-history-of-arts-performing-arts-music\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.humanities", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.humanities", admobTestAppId)
        }
        create("agriculture") {
            dimension = "section"
            applicationId = "ru.cyberleninka.agriculture"
            buildConfigField("String", "SECTION_TITLE", "\"Сельскохозяйственные науки\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"agriculture-forestry-and-fisheries,animal-and-dairy-science,veterinary-science," +
                    "agricultural-biotechnology\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.agriculture", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.agriculture", admobTestAppId)
        }
        create("social") {
            dimension = "section"
            applicationId = "ru.cyberleninka.social"
            buildConfigField("String", "SECTION_TITLE", "\"Социальные науки\"")
            buildConfigField(
                "String", "SECTION_SLUGS",
                "\"psychology,economics-and-business,educational-sciences,sociology,law,political-science," +
                    "social-and-economic-geography,media-and-communications\""
            )
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.social", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.social", admobTestAppId)
        }
        create("full") {
            dimension = "section"
            applicationId = "ru.cyberleninka.full"
            buildConfigField("String", "SECTION_TITLE", "\"Полная библиотека\"")
            buildConfigField("String", "SECTION_SLUGS", "\"*\"")
            buildConfigField("String", "APPODEAL_KEY", "\"${adsProp("appodeal.full", appodealTestKey)}\"")
            manifestPlaceholders["admobAppId"] = adsProp("admob.full", admobTestAppId)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Appodeal
    implementation("com.appodeal.ads.sdk:core:4.4.0")
    implementation("com.appodeal.ads.sdk.adapters:admob:25.2.0.0")
    implementation("com.appodeal.ads.sdk.adapters:amazon:12.0.0.0")
    implementation("com.appodeal.ads.sdk.adapters:applovin_max:13.6.3.1")
    implementation("com.appodeal.ads.sdk.adapters:bidmachine:3.7.1.0")
    implementation("com.appodeal.ads.sdk.adapters:chartboost:9.10.2.0")
    implementation("com.appodeal.ads.sdk.adapters:inmobi:11.3.0.0")
    implementation("com.appodeal.ads.sdk.adapters:ironsource:9.4.4.0")
    implementation("com.appodeal.ads.sdk.adapters:mintegral:17.1.61.0")
    implementation("com.appodeal.ads.sdk.adapters:my_target:5.47.1.0")
    implementation("com.appodeal.ads.sdk.adapters:startio:5.2.4.0")
    implementation("com.appodeal.ads.sdk.adapters:unity_ads:4.17.0.0")
    implementation("com.appodeal.ads.sdk.adapters:vungle:7.7.4.0")
    implementation("com.appodeal.ads.sdk.adapters:yandex:7.17.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
