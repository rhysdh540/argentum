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
    maven("https://maven.cloverclient.com/releases")
    maven("https://maven.bawnorton.com/releases")
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

        rootProject.extra.properties.forEach{ (key, value) ->
            if (key.startsWith("run.")) {
                systemProperties.put(
                    key.removePrefix("run."),
                    value as? String ?: error("run property ${key} is not a string")
                )
            }
        }

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

    modImplementation("pl.tomgirl:lenis:${v("lenis")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${v("devauth")}")
    modImplementation("com.terraformersmc:modmenu:${v("modmenu")}+mc${v("minecraft")}")

    include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${property("mixinsquared_version")}")!!)!!)

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