plugins { id("com.android.library") version "9.5.1" }
val mediaSource = providers.environmentVariable("MEDIA3_SOURCE_DIR").get()
val audioNative = providers.environmentVariable("FFMPEG_JNI_DIR").get()
android {
    namespace = "androidx.media3.decoder.ffmpeg"
    compileSdk = 37
    defaultConfig { minSdk = 21 }
    sourceSets["main"].java.srcDir("$mediaSource/libraries/decoder_ffmpeg/src/main/java")
    sourceSets["main"].jniLibs.srcDir(audioNative)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    api("androidx.media3:media3-decoder:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.annotation:annotation:1.9.1")
    compileOnly("org.checkerframework:checker-qual:3.49.0")
}
