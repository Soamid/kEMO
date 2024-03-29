plugins {
    kotlin("jvm") version "1.4.21"
    idea
}

group = "pl.edu.agh"
version = "1.0-SNAPSHOT"

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
    maven("https://repo.kotlin.link")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "org.moeaframework", name = "moeaframework", version = "2.13")
    implementation(group = "org.slf4j", name="slf4j-api", version= "1.7.30")
    implementation(group = "org.slf4j", name="slf4j-log4j12", version= "1.7.30")
    implementation (group= "me.tongfei", name="progressbar", version="0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.0")
    // https://mvnrepository.com/artifact/me.tongfei/progressbar


    api(group="space.kscience", name="kmath-core", version = "0.2.0")
    implementation("io.kotest:kotest-runner-console-jvm:4.1.3")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.1.3") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.1.3>") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:4.1.3") // for kotest property test
}
