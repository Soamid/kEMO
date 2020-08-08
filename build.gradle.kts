plugins {
    kotlin("jvm") version "1.3.72"
}

group = "pl.edu.agh"
version = "1.0-SNAPSHOT"

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/mipt-npm/scientifik")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "org.moeaframework", name = "moeaframework", version = "2.13")
    api(group="scientifik", name="kmath-core", version = "0.1.3")
    implementation("io.kotest:kotest-runner-console-jvm:4.1.3")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.1.3") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.1.3>") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:4.1.3") // for kotest property test
}
