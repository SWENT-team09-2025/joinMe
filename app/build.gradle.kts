import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("jacoco")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.android.joinme"
    compileSdk = 34
    // Load the API key from local.properties
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""

    defaultConfig {
        applicationId = "com.android.joinme"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        packagingOptions {
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }

    // Robolectric needs to be run only in debug. But its tests are placed in the shared source set (test)
    // The next lines transfers the src/test/* from shared to the testDebug one
    //
    // This prevent errors from occurring during unit tests
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")
        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }

    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

// THE CONFLICTING 'sonar { }' BLOCK HAS BEEN REMOVED FROM HERE

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // --- Unit testing ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    //testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("io.mockk:mockk:1.13.7")
    androidTestImplementation("io.mockk:mockk-android:1.13.7")
    androidTestImplementation("io.mockk:mockk-agent:1.13.7")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // --- Maps ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Firebase
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.firebaseui:firebase-ui-auth:8.0.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")

    // Credential Manager (for Google Sign-In)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // ------------- Jetpack Compose ------------------
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    globalTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.navigation.compose)
    // Material Design 3
    implementation(libs.compose.material3)
    // Integration with activities
    implementation(libs.compose.activity)
    // Integration with ViewModels
    implementation(libs.compose.viewmodel)
    // Android Studio Preview support
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    // UI Tests
    globalTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.manifest)

    // --------- Kaspresso test framework ----------
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // ---------- Robolectric ------------
    testImplementation(libs.robolectric)

    // ---------- WorkManager ------------
    implementation(libs.androidx.work.runtime.ktx)
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf(
            "jdk.internal.*",
            "jdk.proxy.*",
            "**/*$$*"
        )
    }
}


tasks.register("jacocoTestReport", JacocoReport::class) {
    // Note: In CI, tests are already run via 'check' and 'connectedCheck' before this task
    // No dependsOn needed to avoid re-running tests (especially connectedCheck without emulator)
    reports {
        xml.required = true
        html.required = true
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/AuthRepository\$*",
        "**/*\$DefaultImpls*",
        "**/*\$Companion*",
        "**/*\$*Function*"
    )

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
        exclude("jdk.proxy.*", "jdk.internal.*", "**/*$$*")
    }
    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug") {
        exclude(fileFilter)
    }


    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinClasses, javaClasses))

    // FIXED: Use proper fileTree with explicit paths instead of wildcards
    executionData.setFrom(
        fileTree(project.layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
            // Removed: jacoco/testDebugUnitTest.exec (file not generated, data already in outputs/)
        }
    )

    configurations.all {
        resolutionStrategy {
            force("com.google.protobuf:protobuf-javalite:3.25.1")
        }
    }
}

configurations.forEach { configuration ->
    // Exclude protobuf-lite from all configurations
    // This fixes a fatal exception for tests interacting with Cloud Firestore
    configuration.exclude("com.google.protobuf", "protobuf-lite")
}
