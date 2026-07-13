plugins {
    java
    id("net.fabricmc.fabric-loom-remap") version("1.17.+")
    id("ploceus") version("1.17.+")
}

@Suppress("MayBeConstant")
object Versions {
    val minecraft = "1.8.9"
    val feather = "1"
    val osl = "0.20.3"
    val fabric = "0.19.3"
    val lwjgl = "3.4.1"
}

group = "dev.rdh"
version = "0.1"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    exclusiveContent {
        forRepository { mavenCentral() }
        filter {
            includeGroup("org.lwjgl")
        }
    }
    maven("https://maven.taumc.org/releases")
    maven("https://maven.axolotlclient.com/releases")
}

loom.runs.named("client") {
    jvmArguments.add("-XstartOnFirstThread")
    systemProperties.put("java.awt.headless", "true")
    systemProperties.put("legacy_lwjgl3.use_sdl", "true")
}

ploceus {
    setIntermediaryGeneration(2)
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(ploceus.featherMappings(Versions.feather))

    modImplementation("net.fabricmc:fabric-loader:${Versions.fabric}")
    ploceus.dependOsl(Versions.osl)

    include(implementation("org.joml:joml:1.10.5")!!)
    implementation("it.unimi.dsi:fastutil:8.5.15")

    include(implementation("org.embeddedt.celeritas:celeritas-common:2.4.0-dev.5")!!)

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
    modImplementation("io.github.moehreag:legacy-lwjgl3:1.4.0")

//    minecraftRuntimeLibraries("org.taumc:legacy-lwjgl3:20ce025")
//    for (component in arrayOf("lwjgl", "lwjgl-opengl", "lwjgl-openal", "lwjgl-glfw", "lwjgl-stb")) {
//        minecraftLibraries("org.lwjgl:$component:${Versions.lwjgl}")
//        minecraftNatives("org.lwjgl:$component:${Versions.lwjgl}:natives-macos-arm64")
//    }
}

configurations.all {
    resolutionStrategy {
        exclude(group = "org.lwjgl.lwjgl")
    }
    dependencies.removeIf { it.group == "org.lwjgl.lwjgl" }
}

gradle.taskGraph.whenReady {
    allTasks.filter { it.name == "net.fabricmc.devlaunchinjector.Main.main()" }.forEach {
        it.notCompatibleWithConfigurationCache("loom weird?")
    }
}

tasks.assemble {
    dependsOn("remapJar")
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}
