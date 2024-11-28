package net.cchaven.autorestart.commands

import net.cchaven.autorestart.AutoRestart
import net.cchaven.autorestart.utils.ConfigLoader
import net.cchaven.autorestart.utils.Color
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.scheduler.BukkitRunnable

class AutoRestartCommand : CommandExecutor, TabExecutor {
    private val green = Color.GREEN
    private val reset = Color.RESET
    private var restartTask: BukkitTask? = null
    private val restartListener = AutoRestart.restartListener

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (!sender.hasPermission("autorestart")) {
            sender.sendMessage("§cYou do not have permission to use this command")
            return true
        }

        if (args == null || args.isEmpty()) {
            sender.sendMessage("§cUsage: /autorestart <reload | restart | cancel>")
            return true
        }

        try {
            when (args[0]) {
                "reload" -> {
                    AutoRestart.instance.toml = try {
                        ConfigLoader.loadConfig(AutoRestart.instance)
                            ?: throw Exception("Invalid config.toml. Please check the console for the stack trace.")
                    } catch (e: Exception) {
                        sender.sendMessage("§c${e.message}")
                        e.printStackTrace()
                        return true
                    }
                    sender.sendMessage("§aReloaded AutoRestart config")
                    AutoRestart.instance.logger.info("${green}Reloaded AutoRestart config${reset}")
                }

                "restart" -> {
                    if (args.size < 2) {
                        sender.sendMessage("§cYou must specify the delay for the restart.")
                        return true
                    }

                    val delayInMinutes = try {
                        args[1].toInt()
                    } catch (_: NumberFormatException) {
                        sender.sendMessage("§cInvalid delay specified. Please enter a valid number of minutes.")
                        return true
                    }

                    if (delayInMinutes < 1 || delayInMinutes > 10) {
                        sender.sendMessage("§cThe delay must be between 1 and 10 minutes.")
                        return true
                    }

                    AutoRestart.instance.restartDelay = delayInMinutes
                    scheduleRestart(delayInMinutes)
                    restartListener.sendUnplannedRestartMessageToPlayers()
                    sender.sendMessage("§aServer will restart in $delayInMinutes minutes.")
                    AutoRestart.instance.logger.info("${green}Server restart scheduled in $delayInMinutes minutes${reset}")

                }

                "cancel" -> {
                    if (!AutoRestart.instance.isRestartActive) {
                        sender.sendMessage("§cThe server doesn't have any unplanned restarts currently.")
                        return true
                    }
                    cancelRestart()
                    sender.sendMessage("§aServer restart has been cancelled.")
                    AutoRestart.instance.logger.info("${green}Server restart cancelled${reset}")
                    AutoRestart.instance.restartDelay = 0
                }

                else -> {
                    sender.sendMessage("§cUsage: /autorestart <reload | restart | cancel>")
                }
            }
        } catch (e: Exception) {
            sender.sendMessage("§cError: ${e.message}")
            AutoRestart.instance.logger.severe("Error: ${e.message}")
            e.printStackTrace()
        }
        return true
    }

    private fun scheduleRestart(delayMinutes: Int) {
        restartTask?.cancel()
        AutoRestart.instance.restartDelay = delayMinutes
        restartTask = object : BukkitRunnable() {
            override fun run() {
                if (AutoRestart.instance.restartDelay > 0) {
                    broadcastRestartMessage()
                    AutoRestart.instance.restartDelay--

                    if (AutoRestart.instance.restartDelay == 0) {
                        Bukkit.getScheduler().runTask(AutoRestart.instance, Runnable {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop")
                        })
                        this.cancel()
                    }
                }
            }
        }.runTaskTimer(AutoRestart.instance, 0L, 60 * 20L)
    }


    private fun cancelRestart() {
        AutoRestart.instance.isRestartActive = false
        restartTask?.cancel()
        restartTask = null
        restartListener.sendCancelledRestart()
    }

    private fun broadcastRestartMessage() {
        val message = "The server admins have requested a server restart soon! Check the emergency banner for time frame."
        val component = Component.text(message).color(TextColor.fromHexString("#FF0000"))
        Bukkit.getServer().sendMessage(component)
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String>?
    ): List<String> {
        return mutableListOf("reload", "restart", "cancel")
    }
}
