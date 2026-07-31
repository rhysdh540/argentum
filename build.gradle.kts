plugins {
    id("mc")
}

version = "2.4.0-dev.5"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

val testmod = sourceSets.create("testmod")
testmod.compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
testmod.runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath

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
    }
}

dependencies {
    include(implementation("org.joml:joml:1.10.5")!!)
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
    include(api("org.embeddedt.celeritas:celeritas-common:${version}")!!)
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
