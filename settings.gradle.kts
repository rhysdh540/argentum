rootProject.name = "argentum"

include("extras", "cera")

if (file("../celeritas").exists() && !providers.environmentVariable("NO_USE_CELERITAS").isPresent) {
    includeBuild("../celeritas") {
        dependencySubstitution {
            substitute(module("org.embeddedt.celeritas:celeritas-common")).using(project(":common"))
        }
    }
}
