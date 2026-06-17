plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.authplugin"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("AuthPlugin")
        archiveVersion.set(project.version.toString())
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version,
                "name" to "AuthPlugin",
                "main" to "com.authplugin.AuthPlugin"
            )
        }
    }
}
