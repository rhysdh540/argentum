plugins {
    java
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
}

group = rootProject.group
version = "0.1"

base.archivesName = "${rootProject.name}-${project.name}"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

ploceus {
    setIntermediaryGeneration(2)
}

loom.mods {
    create("celeritas") {
        sourceSet(rootProject.sourceSets.main.get())
    }
    create("argentum-extras") {
        sourceSet(sourceSets.main.get())
    }
}

dependencies {
    for (c in arrayOf("minecraft", "mappings", "modImplementation")) {
        rootProject.configurations[c].dependencies.forEach { add(c, it) }
    }
    modImplementation(project(path = ":", configuration = "namedElements"))
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

    runDirectory = rootProject.rootDir.resolve("run")
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)
    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}
