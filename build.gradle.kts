plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")

    // LuckPerms API
    compileOnly("net.luckperms:api:5.4")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
