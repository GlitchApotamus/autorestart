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
        archiveVersion.set("")
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
    dependsOn("generateFingerprint")
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
tasks.register("generateFingerprint") {
    dependsOn(tasks.shadowJar) // Ensure it runs after the shadowJar task

    doLast {
        val jarFile = file("build/libs/autorestart.jar") // Path to your fat JAR
        if (jarFile.exists()) {
            // Generate SHA-256 hash (you can use other algorithms, e.g., MD5 or SHA-1)
            val sha256 = MessageDigest.getInstance("SHA-256")
            val fileBytes = jarFile.readBytes()
            val hashBytes = sha256.digest(fileBytes)
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }

            // Output the hash/fingerprint
            println("Fingerprint (SHA-256): $hashString")

            // Optionally, save the hash to a file
            val fingerprintFile = file("build/libs/autorestart-fingerprint.txt")
            fingerprintFile.writeText(hashString)
        } else {
            println("JAR file not found: ${jarFile.absolutePath}")
        }
    }
}