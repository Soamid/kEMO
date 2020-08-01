plugins {
    kotlin("jvm") version "1.3.72"
}

group = "pl.edu.agh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/mipt-npm/scientifik")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "org.moeaframework", name = "moeaframework", version = "2.13")
    api(group="scientifik", name="kmath-core", version = "0.1.3")
}
