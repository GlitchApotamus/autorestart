package net.cchaven.autorestart.listeners

import net.cchaven.autorestart.AutoRestart
import net.cchaven.autorestart.utils.Color
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.entity.Player
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import java.time.Duration
import java.time.ZonedDateTime

class RestartListener : Listener {
    private val config = AutoRestart.instance.toml.restart
    private val logger = AutoRestart.instance.logger
    private val sentWarnings = AutoRestart.instance.sentWarnings
    private var hoursLeft: Long = 0
    private var minutesLeft: Long = 0
    private var warnMinutesLeft: Long = 0
    private var lastRestarted: ZonedDateTime? = null

    init {
        startListener()
    }

    private fun startListener() {
        object : BukkitRunnable() {
            override fun run() {
                val now = ZonedDateTime.now()
                val nextExecution = getNextExecutionTime(now) ?: return
                updateDurations(now, nextExecution)
                handleWarnings()
                if (shouldRestart(now, nextExecution)) triggerRestart()
            }
        }.runTaskTimer(AutoRestart.instance, 0L, 20L * 60)
    }

    private fun getNextExecutionTime(now: ZonedDateTime): ZonedDateTime? {
        val cron = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(config.cron)
        return ExecutionTime.forCron(cron).nextExecution(now).orElse(null)
    }

    private fun updateDurations(now: ZonedDateTime, nextExecution: ZonedDateTime) {
        val duration = Duration.between(now, nextExecution)
        hoursLeft = duration.toHours()
        minutesLeft = duration.toMinutes() % 60
        warnMinutesLeft = duration.toMinutes()
    }

    private fun handleWarnings() {
        val currentMinutesLeft = warnMinutesLeft.toInt()
        config.warningIntervals.filter { it == currentMinutesLeft && it !in sentWarnings }.forEach { interval ->
            sendWarningMessage(formatTimeLeft(interval / 60, interval % 60))
            sentWarnings.add(interval)
        }
        if (currentMinutesLeft < 0) sentWarnings.clear()
    }

    private fun shouldRestart(now: ZonedDateTime, nextExecution: ZonedDateTime): Boolean {
        if (now.isAfter(nextExecution.minusMinutes(1)) && now.isBefore(nextExecution.plusMinutes(1))) {
            if (lastRestarted != nextExecution) {
                lastRestarted = nextExecution
                return true
            }
        }
        return false
    }

    private fun triggerRestart() {
        if (minutesLeft < 1 && !AutoRestart.instance.restartSent) {
            sendBroadcastMessage("The server is now restarting!", Color.RED)
            AutoRestart.instance.restartSent = true
            Bukkit.getScheduler().runTask(AutoRestart.instance, Runnable {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), config.command)
            })
        }
    }

    private fun sendWarningMessage(message: String) {
        sendBroadcastMessage("Warning: The server will restart in $message!", Color.YELLOW)
    }

    private fun sendBroadcastMessage(message: String, color: String) {
        val formatted = createColorMessage(message, color)
        Bukkit.broadcast(Component.text(formatted))
        logger.info(getAnsiColor(color) + formatted.substring(2) + getAnsiColor(Color.RESET))
    }

    private fun createColorMessage(message: String, color: String): String {
        return when (color) {
            Color.GREEN -> "§a$message"
            Color.RED -> "§c$message"
            Color.YELLOW -> "§e$message"
            else -> "§f$message"
        }
    }

    private fun getAnsiColor(color: String): String {
        return when (color) {
            Color.GREEN -> "\u001B[32m"
            Color.RED -> "\u001B[31m"
            Color.YELLOW -> "\u001B[33m"
            Color.RESET -> "\u001B[0m"
            else -> ""
        }
    }

    private fun formatTimeLeft(hours: Int, minutes: Int): String {
        return when {
            hours > 0 && minutes > 0 -> "$hours hour${if (hours > 1) "s" else ""} and $minutes minute${if (minutes > 1) "s" else ""}"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            else -> "$minutes minute${if (minutes > 1) "s" else ""}"
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val now = ZonedDateTime.now()
        val nextExecution = getNextExecutionTime(now) ?: return
        val timeLeft = Duration.between(now, nextExecution).toMinutes()

        val message = when {
            AutoRestart.instance.isManualRestart -> "An unplanned restart is scheduled in ${AutoRestart.instance.restartDelay} minute${if (AutoRestart.instance.restartDelay > 1) "s" else ""}. Please prepare."
            timeLeft in 1..30 -> "The server is restarting in ${formatTimeLeft(hoursLeft.toInt(), minutesLeft.toInt())}! Please prepare."
            else -> "No imminent restart. Next restart is in ${formatTimeLeft(hoursLeft.toInt(), minutesLeft.toInt())}."
        }
        event.player.sendMessage(createColorMessage(message, if (timeLeft in 1..30) Color.YELLOW else Color.GREEN))
    }

    fun sendUnplannedRestartMessageToPlayers() {
        AutoRestart.instance.isManualRestart = true
        AutoRestart.instance.isRestartActive = true

        object : BukkitRunnable() {
            var remaining = AutoRestart.instance.restartDelay
            override fun run() {
                if (!AutoRestart.instance.isRestartActive || remaining < 0) cancel()
                val message = if (remaining == 0) "The server is restarting now!" else "The server is restarting in $remaining minute${if (remaining > 1) "s" else ""}! Please prepare."
                Bukkit.getOnlinePlayers().forEach { sendActionBar(it, message) }
                remaining--
            }
        }.runTaskTimer(AutoRestart.instance, 0L, 20L * 60)
    }

    private fun sendActionBar(player: Player, message: String) {
        player.sendActionBar(Component.text(message).color(TextColor.fromHexString("#FF0000")))
    }

    fun sendCancelledRestart() {
        AutoRestart.instance.isManualRestart = false
        sendBroadcastMessage("The unplanned server restart has been cancelled. Continue as you were!", Color.GREEN)
    }
}
