plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.ornithemc.net/releases")
    mavenCentral()
}

val loomVersion = "1.17.+"

dependencies {
    implementation("net.fabricmc:fabric-loom:$loomVersion")
    implementation("net.ornithemc:ploceus:$loomVersion")
}