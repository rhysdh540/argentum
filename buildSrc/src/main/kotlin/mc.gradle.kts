plugins {
    id("java")
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
}

group = "dev.rdh"

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

loom {
    uncompressNestedJars = true

    setOf(project, rootProject).forEach {
        mods.create(it.name) {
            sourceSet(it.sourceSets.main.get())
        }
    }
    runs.named("client") {
        jvmArguments.add("-XstartOnFirstThread")

        jvmArguments.add("-XX:+UseZGC")
        jvmArguments.add("-XX:MaxGCPauseMillis=50")
        jvmArguments.add("-XX:+UseCompactObjectHeaders")
        jvmArguments.add("--enable-native-access=ALL-UNNAMED")
        jvmArguments.add("--sun-misc-unsafe-memory-access=allow")

        systemProperties.put("java.awt.headless", "true")
        systemProperties.put("legacy_lwjgl3.use_sdl", "true")
        systemProperties.put("devauth.enabled", "true")
        systemProperties.put("mixin.debug.export", "true")

        if (project != rootProject) {
            runDirectory = rootProject.layout.projectDirectory.dir("run")
        }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

if (project != rootProject) {
    tasks.remapJar {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
    }
}

fun v(n: String) = rootProject.property("${n}_version") as? String ?: error("no property ${n}_version")

dependencies {
    minecraft("com.mojang:minecraft:${v("minecraft")}")
    mappings(loom.layered {
        mappings(ploceus.featherMappings(v("feather")))
        mappings(rootProject.file("mappings/feather-overrides.tiny"))
    })

    modImplementation("net.fabricmc:fabric-loader:${v("fabric")}")
    ploceus.dependOsl(v("osl"))

    modImplementation("io.github.moehreag:legacy-lwjgl3:${v("legacy_lwjgl3")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${v("devauth")}")

    if (project != rootProject) {
        implementation(project(path = ":", configuration = "namedElements"))
    }
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }

    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
}