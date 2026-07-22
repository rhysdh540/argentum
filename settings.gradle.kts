pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.ornithemc.net/releases")
        mavenCentral()
    }
}

rootProject.name = "argentum"

include("extras")
