plugins {
    kotlin("jvm") version "1.3.72"
}

group = "pl.edu.agh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group="org.moeaframework", name="moeaframework", version="2.13")
}
