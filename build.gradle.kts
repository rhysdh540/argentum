plugins {
    id("mc")
}

version = "1.0.0"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

val testmod = sourceSets.create("testmod")
testmod.compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
testmod.runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath

gradle.taskGraph.whenReady {
    allTasks.filter { it.name == "net.fabricmc.devlaunchinjector.Main.main()" }.forEach {
        it.notCompatibleWithConfigurationCache("loom weird?")
    }
}

loom {
    accessWidenerPath = file("src/main/resources/argentum.classtweaker")

    mods {
        create("argentum-font-test") {
            sourceSet(testmod)
        }
    }
    runs {
        create("fontTestClient") {
            inherit(getByName("client"))
            sourceSet = testmod.name
            displayName = "Font Visual Test"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("argentum.disableFontBatching", findProperty("disableFontBatching")?.toString() ?: "false")
            systemProperties.put("argentum.fontTestVariant", findProperty("fontTestVariant")?.toString() ?: "batched")
        }
        create("fontTestVanillaClient") {
            inherit(getByName("client"))
            sourceSet = testmod.name
            displayName = "Font Visual Test (Vanilla)"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("argentum.disableFontBatching", "true")
            systemProperties.put("argentum.fontTestVariant", "vanilla")
        }
        create("guiBenchmarkBatchedClient") {
            inherit(getByName("client"))
            sourceSet = testmod.name
            displayName = "GUI Batch Benchmark (Batched)"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("argentum.guiBenchmark", "true")
            systemProperties.put("argentum.guiBenchmarkBatched", "true")
        }
        create("guiBenchmarkUnbatchedClient") {
            inherit(getByName("client"))
            sourceSet = testmod.name
            displayName = "GUI Batch Benchmark (Unbatched)"
            jvmArguments.add("-XstartOnFirstThread")
            systemProperties.put("argentum.guiBenchmark", "true")
            systemProperties.put("argentum.guiBenchmarkBatched", "false")
        }
    }
}

dependencies {
    include(api("org.joml:joml:1.10.5")!!)
    include(api("org.embeddedt.celeritas:celeritas-common:${property("celeritas_version")}")!!)
}

tasks.named("runFontTestClient") {
    mustRunAfter("runFontTestVanillaClient")
}

tasks.named("runGuiBenchmarkBatchedClient") {
    mustRunAfter("runGuiBenchmarkUnbatchedClient")
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

tasks.register("benchmarkGuiBatching") {
    group = "verification"
    dependsOn("runGuiBenchmarkUnbatchedClient", "runGuiBenchmarkBatchedClient")
}

tasks.check {
    dependsOn("compileTestmodJava")
}
