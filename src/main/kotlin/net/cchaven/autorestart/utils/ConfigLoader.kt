package net.cchaven.autorestart.utils

import cc.ekblad.toml.decode
import org.bukkit.plugin.java.JavaPlugin
import cc.ekblad.toml.tomlMapper
import java.io.File
import java.nio.file.Path

data class RestartConfig(
    val cron: String,
    val command: String,
    val warningIntervals: List<Int>
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
                    # Cron expression for scheduling automatic restarts (e.g., "0 0 3 * * *" for every 3 AM)
                    cron = "0 0 3 * * *"
                    # what command to run to restart the server. make sure you have a ./start.sh or ./start.bat file to use restart
                    command = "restart"
                    # List of warning intervals (in minutes) before the restart
                    warningIntervals = [30, 20, 15, 10, 5, 3, 1]

   
                """.trimIndent()
            )
        }

        tomlMapper { }.decode<Config>(Path.of(filePath.toURI()))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}