// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.sonar) apply true
}

sonarqube {
    properties {
        property("sonar.projectKey", "SWENT-team09-2025_joinMe")
        property("sonar.organization", "swent-team09-2025")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java,app/src/androidTest/java")
        property("sonar.java.binaries", "app/build/tmp/kotlin-classes/debug")
        property("sonar.java.test.binaries", "app/build/tmp/kotlin-classes/debugUnitTest,app/build/tmp/kotlin-classes/debugAndroidTest")
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.exclusions", "**/*.png,**/*.jpg,**/*.jpeg,**/*.webp,**/*.gif,**/*.svg")
    }
}