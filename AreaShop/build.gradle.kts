plugins {
    id("io.github.goooler.shadow") version "8.1.7"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

idea {
    module {
        isDownloadSources = true
    }
}

description = "AreaShop"

dependencies {
    // Platform
    compileOnlyApi(libs.spigot)
    compileOnlyApi(libs.worldeditBukkit)
    compileOnlyApi(libs.worldguardBukkit)
    compileOnlyApi("com.github.MilkBowl:VaultAPI:1.7")
    compileOnlyApi("me.clip:placeholderapi:2.11.6")

    // 3rd party libraries
    api("io.papermc:paperlib:1.0.8")
    api("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    api("com.github.NLthijs48:BukkitDo:819d51ec2b")
    api("io.github.baked-libs:dough-data:1.2.0")
    api("com.google.inject:guice:7.0.0") {
        exclude("com.google.guava")
    }
    api("com.google.inject.extensions:guice-assistedinject:7.0.0") {
        exclude("com.google.guava")
    }
    implementation("org.incendo:cloud-paper:2.0.0-beta.9") {
        exclude("com.google.guava")
    }
    implementation("org.incendo:cloud-processors-confirmation:1.0.0-beta.3") {
        exclude("com.google.guava")
    }
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.2")
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    implementation("org.jetbrains:annotations:23.0.0")

    // Project submodules
    api(projects.areashopInterface)
    api(projects.adapters.platform.platformInterface)
    api(projects.adapters.platform.paper)

    if (!providers.environmentVariable("JITPACK").isPresent) {
        // We don't need these adapters if we are only publishing an api jar
        runtimeOnly(projects.adapters.plugins.worldedit)
        runtimeOnly(projects.adapters.plugins.worldguard)
        runtimeOnly(projects.adapters.plugins.fastasyncworldedit)
        runtimeOnly(projects.adapters.plugins.essentials)
    }

    testImplementation("com.github.MilkBowl:VaultAPI:1.7")
    testImplementation("com.github.MilkBowl:VaultAPI:1.7")
    testImplementation("me.clip:placeholderapi:2.11.6")
    testImplementation(libs.worldeditBukkit)
    testImplementation(libs.worldguardBukkit)
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.131.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    assemble {
        if (!providers.environmentVariable("JITPACK").isPresent) {
            dependsOn(shadowJar)
        }
    }

    jar {
        archiveBaseName.set("AreaShop")
        if (!providers.environmentVariable("JITPACK").isPresent) {
            archiveClassifier.set("original")
        } else {
            archiveClassifier.set("")
        }
    }

    if (providers.environmentVariable("JITPACK").isPresent) {
        artifacts {
            archives(jar)
        }
    }

    java {
        withSourcesJar()
    }

    val javaComponent = project.components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }

    shadowJar {
        archiveClassifier.set("")
        base {
            archiveBaseName.set("AreaShop")
        }
        val base = "me.wiefferink.areashop.libraries"
        relocate("org.incendo.cloud", "${base}.cloud")
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("com.google.errorprone", "${base}.errorprone")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.annotation", "${base}.javax.annotation")
        relocate("javax.inject", "${base}.javax.inject")
        relocate("org.jetbrains.annotation", "${base}.jetbrains.annotations")
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")

        downloadPlugins {
            github("EssentialsX", "essentials", "2.20.1", "EssentialsX-2.20.1.jar")
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            // WorldEdit 7.3.3
            url("https://mediafilez.forgecdn.net/files/5400/331/worldedit-bukkit-7.3.3.jar")
            // WorldGuard 7.0.10
            url("https://mediafilez.forgecdn.net/files/5344/377/worldguard-bukkit-7.0.10-dist.jar")
        }
    }
}
