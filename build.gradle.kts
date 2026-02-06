import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2" // Adds runServer and runMojangMappedServer tasks for testing
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0" // Generates plugin.yml based on the Gradle config
}

group = "xyz.lapismc.exsurvival"
version = "1.0.0-SNAPSHOT"
description = "The EXSurvival Plugin"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

// For 1.20.4 or below, or when you care about supporting Spigot on >=1.20.5:

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
    dependsOn(tasks.reobfJar)
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    // paperweight.foliaDevBundle("1.21.11-R0.1-SNAPSHOT")
    // paperweight.devBundle("com.example.paperfork", "1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 21
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    // Only relevant for 1.20.4 or below, or when you care about supporting Spigot on >=1.20.5:
    reobfJar {
        // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
        // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
        outputJar = layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar")
    }
}

// Configure plugin.yml generation
// - name, version, and description are inherited from the Gradle project.
bukkitPluginYaml {
    main = "xyz.lapismc.exsurvival.EXSurvivalPlugin"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.add("MesuDevastator")
    apiVersion = "1.21.11"

    commands {
        register("exsurvival") {
            description = "EXSurvival main command"
            aliases = listOf("exs")
            usage = "/exsurvival <start|stop> [world-id]"
        }
    }

    permissions {
        register("exsurvival.control") {
            description = "Allows starting and controlling EXSurvival games"
            default = Permission.Default.OP
        }
        register("exsurvival.reload") {
            description = "Allows reloading EXSurvival configuration"
            default = Permission.Default.OP
        }
    }
}
