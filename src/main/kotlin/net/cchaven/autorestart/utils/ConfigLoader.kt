package net.cchaven.autorestart.utils

import cc.ekblad.toml.decode
import org.bukkit.plugin.java.JavaPlugin
import cc.ekblad.toml.tomlMapper
import java.io.File
import java.nio.file.Path

data class RestartConfig(
    val cron: String,
    val command: String
)

data class Config(
    val restart: RestartConfig,
)

object ConfigLoader {
    fun loadConfig(plugin: JavaPlugin) = try {
        val filePath = File(plugin.dataFolder, "config.toml")

        if (!filePath.exists()) {
            filePath.parentFile.mkdirs()
            filePath.writeText(
                """
                    # AutoRestart Configuration
                    # This file is written in TOML format (https://toml.io/en/)
                    # You can also use placeholder values, specified in the comments below for each event specifically
                    
                    # Change options below to specify when to restart the server
                    [restart]
                    cron = "* * * * *" 
                    command = "restart"
   
                """.trimIndent()
            )
        }

        tomlMapper { }.decode<Config>(Path.of(filePath.toURI()))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}