pluginManagement {
    repositories {
        // Repositórios para resolver *plugins* (AGP, Kotlin, Compose, etc.)
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Mantém a resolução automática de toolchains (Java 17)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    // Usa apenas os repositórios definidos acima (evita repositórios por módulo)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Repositórios para as dependências normais do app
        google()
        mavenCentral()
    }
}

rootProject.name = "Abraham"
include(":app")
