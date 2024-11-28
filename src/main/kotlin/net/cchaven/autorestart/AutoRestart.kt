package net.cchaven.autorestart

import net.cchaven.autorestart.commands.*
import net.cchaven.autorestart.utils.Config
import net.cchaven.autorestart.utils.ConfigLoader
import org.bukkit.plugin.java.JavaPlugin
import net.cchaven.autorestart.listeners.*
import net.cchaven.autorestart.utils.Color

@Suppress("SameParameterValue")
class AutoRestart : JavaPlugin() {
    var isRestartActive: Boolean = false
    lateinit var toml: Config
    var restartDelay: Int = 0
    var isManualRestart: Boolean = false
    var restartSent: Boolean = false
    val sentWarnings = mutableSetOf<Int>()

    override fun onEnable() {
        instance = this
        toml = try {
            ConfigLoader.loadConfig(this)!!
        } catch (e: Exception) {
            logger.severe("Failed to load config.toml: ${e.message}")
            e.printStackTrace()
            return
        }
        restartListener = RestartListener()


        val commands = arrayOf("autorestart" to AutoRestartCommand())
        val events = arrayOf(RestartListener())

        logger.info("${Color.GREEN}┌────────────────────────────────┐${Color.RESET}")
        logger.info("${Color.GREEN}│    ___    ____      ☐ ☐ ☐      │${Color.RESET}")
        logger.info("${Color.GREEN}│   / _ \\  |  _ \\     ☐ ☐ ☐      │${Color.RESET}")
        logger.info("${Color.GREEN}│  / /_\\ \\ | |_) |    ☐ ☐ ☐      │${Color.RESET}")
        logger.info("${Color.GREEN}│ /  _  \\ \\|  _ <     ☐ ☐ ☐      │${Color.RESET}")
        logger.info("${Color.GREEN}│/_/   \\_\\_\\_| \\_\\   (❁´◡`❁)     │${Color.RESET}")
        logger.info("${Color.GREEN}│ Running on Bukkit - Paper      │${Color.RESET}")
        logger.info("${Color.GREEN}└────────────────────────────────┘${Color.RESET}")

        fun logWithBox(vararg logMessages: String) {
            val maxLength = logMessages.maxOf { it.length }

            val border = "┌" + "─".repeat(maxLength + 2) + "┐"
            val bottomBorder = "└" + "─".repeat(maxLength + 2) + "┘"

            logger.info("${Color.GREEN}$border${Color.RESET}")

            logMessages.forEach { message ->
                val paddedMessage = "│ ${message.padEnd(maxLength)} │"
                logger.info("${Color.GREEN}$paddedMessage${Color.RESET}")
            }

            logger.info("${Color.GREEN}$bottomBorder${Color.RESET}")
        }

        fun logAllCommandsAndEvents() {
            val logMessages = mutableListOf<String>()

            commands.forEach {
                getCommand(it.first)?.setExecutor(it.second)
                getCommand("autorestart")?.tabCompleter = AutoRestartCommand()
                logMessages.add("Loaded Command: ${it.first}")
            }

            events.forEach {
                server.pluginManager.registerEvents(it, this)
                logMessages.add("Registered Event: ${it::class.java.simpleName}")
            }

            logMessages.add("Enabled Plugin")

            logWithBox(*logMessages.toTypedArray())
        }

        logAllCommandsAndEvents()


    }

    override fun onDisable() {
        if (!this.server.isStopping) return

        val lines = listOf(
            "Plugin Disabled"
        )

        logBox(lines, Color.RED)

    }

    private fun logBox(messages: List<String>, borderColor: String) {
        val maxLength = messages.maxOf { it.length }
        val topBorder = "${borderColor}╔" + "═".repeat(maxLength + 2) + "╗${Color.RESET}"
        val bottomBorder = "${borderColor}╚" + "═".repeat(maxLength + 2) + "╝${Color.RESET}"
        val formatLine: (String) -> String = { "${borderColor}║ ${it.padEnd(maxLength)} ${borderColor}║${Color.RESET}" }

        logger.info(topBorder)
        messages.forEach { logger.info(formatLine(it)) }
        logger.info(bottomBorder)
    }



    companion object {
        lateinit var restartListener: RestartListener
        lateinit var instance: AutoRestart
            private set
    }
}
