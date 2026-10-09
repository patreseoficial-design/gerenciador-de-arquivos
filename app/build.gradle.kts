plugins {
    id("com.android.application") version "8.13.0"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

// Assinatura da versão da Play Store: os dados vêm dos "Secrets"
// do GitHub (veja docs/PUBLICAR-NA-PLAY-STORE.md). Sem eles, só a
// versão de teste (debug) é gerada.
val arquivoChave = System.getenv("KEYSTORE_FILE")
val temChave = !arquivoChave.isNullOrBlank() && file(arquivoChave).exists()

fun idAdMob(nome: String, teste: String): String =
    System.getenv(nome)?.takeIf { it.isNotBlank() } ?: teste

android {
    namespace = "com.gerenciadordearquivos.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gerenciadordearquivos.app"
        minSdk = 23
        targetSdk = 35

        // Cada envio para a Play Store precisa de um número maior:
        // usa o número da execução do GitHub Actions
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0.${System.getenv("VERSION_CODE") ?: "0"}"

        // IDs do AdMob: vêm dos Secrets do GitHub. Sem eles, usa os
        // IDs de TESTE do Google (mostram anúncios de exemplo e não
        // geram dinheiro). Veja docs/PUBLICAR-NA-PLAY-STORE.md.
        resValue(
            "string",
            "admob_app_id",
            idAdMob("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        )
        resValue(
            "string",
            "admob_banner_id",
            idAdMob("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/9214589741")
        )
        resValue(
            "string",
            "admob_intersticial_id",
            idAdMob("ADMOB_INTERSTICIAL_ID", "ca-app-pub-3940256099942544/1033173712")
        )
    }

    buildFeatures {
        resValues = true
    }

    signingConfigs {
        if (temChave) {
            create("release") {
                storeFile = file(arquivoChave!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (temChave) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    // Compra do Premium pela Google Play
    implementation("com.android.billingclient:billing:8.0.0")

    // Anúncios (AdMob) e tela de consentimento de privacidade
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
