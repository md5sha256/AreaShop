pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}

rootProject.name = "areashop-parent"

// Interfaces
include(":areashop-interface")

// Adapters
include(":adapters:platform:paper")
include(":adapters:platform:platform-interface")

include(":adapters:plugins:worldedit")
include(":adapters:plugins:worldguard")
include(":adapters:plugins:fastasyncworldedit")

include(":adapters:plugins:essentials")


// Main project
include(":areashop")

project(":areashop").projectDir = file("AreaShop")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
