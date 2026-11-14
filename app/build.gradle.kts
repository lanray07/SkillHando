plugins {
    id("com.android.application") version "8.11.1"
    id("org.jetbrains.kotlin.android") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.kotlin.kapt") version "2.1.20"
}

abstract class CopyAmazonKey : DefaultTask() {
    @get:InputFile abstract val publicKey: RegularFileProperty
    @get:OutputDirectory abstract val assetsDir: DirectoryProperty

    @TaskAction
    fun copyKey() {
        val destination = assetsDir.file("AppstoreAuthenticationKey.pem").get().asFile
        publicKey.get().asFile.copyTo(destination, overwrite = true)
    }
}

android {
    namespace = "com.skillhando.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skillhando.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val sku = providers.gradleProperty("skillhandoAmazonSku").orElse("").get()
        val verifier = providers.gradleProperty("skillhandoVerifierUrl").orElse("").get()
        require(sku.isEmpty() || sku.matches(Regex("[A-Za-z0-9_.-]{1,150}")))
        require(verifier.isEmpty() || verifier.matches(Regex("https://[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9/_-]+")))
        buildConfigField("String", "AMAZON_SKU", "\"$sku\"")
        buildConfigField("String", "VERIFIER_URL", "\"$verifier\"")
    }

    defaultConfig { buildConfigField("boolean", "BILLING_SANDBOX", "false") }
    val billingSandbox = providers.gradleProperty("skillhandoBillingSandbox").orNull == "true"
    val screenshotBuild = providers.gradleProperty("skillhandoScreenshotBuild").orNull == "true"
    require(!(billingSandbox && screenshotBuild))
    buildTypes.getByName("debug") {
        if (billingSandbox) {
            applicationIdSuffix = ".billingtest"
            buildConfigField("boolean", "BILLING_SANDBOX", "true")
            buildConfigField("String", "AMAZON_SKU", "\"com.skillhando.testing.premium\"")
            buildConfigField("String", "VERIFIER_URL", "\"http://127.0.0.1:18744/verify\"")
        }
        if (screenshotBuild) {
            applicationIdSuffix = ".screenshots"
        }
    }

    if (billingSandbox) {
        sourceSets.getByName("debug").manifest.srcFile("src/billingSandbox/AndroidManifest.xml")
        sourceSets.getByName("debug").res.srcDir("src/billingSandbox/res")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

providers.gradleProperty("skillhandoAmazonAuthFile").orNull?.let { path ->
    val publicKeyFile = layout.projectDirectory.file(path)
    androidComponents.onVariants { variant ->
        val copyKey = tasks.register<CopyAmazonKey>("copyAmazonKeyFor${variant.name.replaceFirstChar { it.uppercaseChar() }}") {
            publicKey.set(publicKeyFile)
        }
        requireNotNull(variant.sources.assets).addGeneratedSourceDirectory(copyKey, CopyAmazonKey::assetsDir)
    }
}

kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.amazon.device:amazon-appstore-sdk:3.0.9")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
