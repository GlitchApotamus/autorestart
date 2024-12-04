import java.security.MessageDigest

val versionFile = file("version.txt")

fun incrementVersion(version: String): String {
    val parts = version.split(".").map { it.toInt() }
    val major = parts[0]
    var minor = parts[1]
    var patch = parts[2]

    if (patch < 9) {
        patch++
    } else {
        patch = 0
        if (minor < 9) {
            minor++
        } else {
            minor = 0
        }
    }

    return "$major.$minor.$patch"
}

val currentVersion = versionFile.readText().trim()
val newVersion = incrementVersion(currentVersion)

plugins {
    kotlin("jvm") version "2.1.0-Beta2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "net.cchaven"
version = newVersion
versionFile.writeText(newVersion)

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}



dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("cc.ekblad:4koma:1.2.0")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("net.kyori:adventure-api:4.9.3")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}
tasks.jar {enabled = false}
tasks {
    shadowJar {
        archiveBaseName.set("autorestart")
//        archiveVersion.set("")
        archiveClassifier.set("")
        manifest {
            attributes(
                "Main-Class" to "net.cchaven.autorestart.AutoRestart"
            )
        }
    }
}

tasks.build {
    dependsOn("shadowJar")
    dependsOn("showVersion")
//    dependsOn("publishAutorestartPublicationToAutorestartRepository")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to newVersion)
    }
}
// ENSURE YOU HAVE bash SET IN YOUR env table!
tasks.register("deploy") {
    exec {
        commandLine("bash", "upload.sh")
    }
}
tasks.register("showVersion") {
    doLast {
        println("Current plugin version: $newVersion")
    }
}

// Fill in mavenUser and mavenPassword in ./gradle.properties or MAVEN_USER AND MAVEN_PASSWORD in your system environment.
// Then uncomment dependsOn("publishAutorestartPublicationToAutorestartRepository") in tasks.build {}
publishing {
    publications {
        create<MavenPublication>("autorestart") {
            artifactId = "autorestart"
            from(components["java"])
            artifacts.clear()
            artifact(file("build/libs/autorestart-${newVersion}.jar"))
        }
    }
    repositories {
        maven {
            name = "autorestart"
            url = uri("${project.findProperty("uri") as String?}")
            credentials {
                username = project.findProperty("mavenUser") as String? ?: System.getenv("MAVEN_USER")
                password = project.findProperty("mavenPassword") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}