plugins {
    `java-library`
    id("net.fabricmc.fabric-loom-remap") version("1.17.+")
    id("ploceus") version("1.17.+")
}

@Suppress("MayBeConstant")
object Versions {
    val minecraft = "1.8.9"
    val feather = "1"
    val osl = "0.20.3"
    val fabric = "0.19.3"
    val legacy_lwjgl3 = "1.4.0"
}

group = "dev.rdh"
version = "2.4.0-dev.5"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

val testmod = sourceSets.create("testmod")
testmod.compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
testmod.runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath

allprojects {
    apply(plugin = "java")

    repositories {
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("org.lwjgl")
            }
        }
        maven("https://maven.taumc.org/releases")
        maven("https://maven.axolotlclient.com/releases")
        exclusiveContent {
            forRepository { maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
            filter {
                includeGroup("me.djtheredstoner")
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            exclude(group = "org.lwjgl.lwjgl")
        }
    }

    gradle.taskGraph.whenReady {
        allTasks.filter { it.name == "net.fabricmc.devlaunchinjector.Main.main()" }.forEach {
            it.notCompatibleWithConfigurationCache("loom weird?")
        }
    }

    tasks.assemble {
        dependsOn("remapJar")
    }
}

loom.runs.named("client") {
    jvmArguments.add("-XstartOnFirstThread")

    jvmArguments.add("-XX:+UseZGC")
    jvmArguments.add("-XX:MaxGCPauseMillis=50")
    jvmArguments.add("-XX:+UseCompactObjectHeaders")
    jvmArguments.add("--enable-native-access=ALL-UNNAMED")
    jvmArguments.add("--sun-misc-unsafe-memory-access=allow")

    systemProperties.put("java.awt.headless", "true")
    systemProperties.put("legacy_lwjgl3.use_sdl", "true")
    systemProperties.put("devauth.enabled", "true")
}

loom {
    mods {
        create("celeritas") {
            sourceSet(sourceSets.main.get())
        }
        create("celeritas-font-test") {
            sourceSet(testmod)
        }
    }
    runs {
        create("fontTestClient") {
            inherit(getByName("client"))
            source(testmod)
            configName = "Font Visual Test"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("celeritas.disableFontBatching", findProperty("disableFontBatching")?.toString() ?: "false")
            systemProperties.put("celeritas.fontTestVariant", findProperty("fontTestVariant")?.toString() ?: "batched")
        }
        create("fontTestVanillaClient") {
            inherit(getByName("client"))
            source(testmod)
            configName = "Font Visual Test (Vanilla)"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("celeritas.disableFontBatching", "true")
            systemProperties.put("celeritas.fontTestVariant", "vanilla")
        }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(loom.layered {
        mappings(ploceus.featherMappings(Versions.feather))
        mappings(file("mappings/feather-overrides.tiny"))
    })

    modImplementation("net.fabricmc:fabric-loader:${Versions.fabric}")
    ploceus.dependOsl(Versions.osl)

    include(implementation("org.joml:joml:1.10.5")!!)
    implementation("it.unimi.dsi:fastutil:8.5.15")

    include(api("org.embeddedt.celeritas:celeritas-common:${version}")!!)

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
    modImplementation("io.github.moehreag:legacy-lwjgl3:${Versions.legacy_lwjgl3}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
}

tasks.named("runFontTestClient") {
    mustRunAfter("runFontTestVanillaClient")
}

fun registerFontComparison(name: String, expected: String, actual: String) = tasks.register<Exec>(name) {
    dependsOn("runFontTestVanillaClient", "runFontTestClient")
    commandLine("cmp", "run/screenshots/$expected", "run/screenshots/$actual")
}

val verifyFontBeforeReload = registerFontComparison(
    "verifyFontBeforeReload",
    "font-vanilla-before-reload.png",
    "font-batched-before-reload.png"
)
val verifyFontAfterReload = registerFontComparison(
    "verifyFontAfterReload",
    "font-vanilla-after-reload.png",
    "font-batched-after-reload.png"
)
val verifyFontReload = registerFontComparison(
    "verifyFontReload",
    "font-batched-before-reload.png",
    "font-batched-after-reload.png"
)

tasks.register("verifyFontRendering") {
    group = "verification"
    dependsOn(verifyFontBeforeReload, verifyFontAfterReload, verifyFontReload)
}

tasks.check {
    dependsOn("compileTestmodJava")
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}
