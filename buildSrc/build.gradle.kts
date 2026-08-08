plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.ornithemc.net/releases")
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:fabric-loom:1.17.19")
    implementation("net.ornithemc:ploceus:1.17.6")
}