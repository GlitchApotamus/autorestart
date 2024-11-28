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

    private val restartConfig = AutoRestart.instance.toml.restart
    private val logger = AutoRestart.instance.logger
    private val warningIntervals = listOf(30, 20, 15, 10, 5, 3, 1)
    private var hoursLeft: Long = 0
    private var minutesLeft: Long = 0
    private val sentWarnings = AutoRestart.instance.sentWarnings

    init {
        startListener()

    }

    private fun startListener() {
        object : BukkitRunnable() {
            override fun run() {
                val now = ZonedDateTime.now()
                val nextExecutionTime = getNextExecutionTime(now) ?: return
                updateDurationUntilRestart(now, nextExecutionTime)
                sendWarns()
                if (shouldTriggerRestart(now, nextExecutionTime)) triggerRestart()
            }
        }.runTaskTimer(AutoRestart.instance, 0L, 20L * 60)

    }

    private fun getNextExecutionTime(now: ZonedDateTime): ZonedDateTime? {
        val cron = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(restartConfig.cron)
        return ExecutionTime.forCron(cron).nextExecution(now).orElse(null)
    }


    private fun updateDurationUntilRestart(now: ZonedDateTime, nextExecutionTime: ZonedDateTime) {
        val durationUntilRestart = Duration.between(now, nextExecutionTime)
        hoursLeft = durationUntilRestart.toHours()
        minutesLeft = durationUntilRestart.toMinutes() % 60
    }

    private fun sendWarns() {
        val currentMinutesLeft = minutesLeft.toInt()

        warningIntervals.filter { it == currentMinutesLeft && it !in sentWarnings }.forEach {

            sendWarningMessage(it)
            sentWarnings.add(it)
        }

        if (currentMinutesLeft < 0) {
            sentWarnings.clear()
        }
    }

    private fun shouldTriggerRestart(now: ZonedDateTime, nextExecutionTime: ZonedDateTime): Boolean {
        return now.isAfter(nextExecutionTime.minusMinutes(1)) && now.isBefore(nextExecutionTime.plusMinutes(1))
    }

    private fun triggerRestart() {
        if ((minutesLeft < 1) && !AutoRestart.instance.restartSent) {
            val message = "The server is now restarting!"
            broadcastRestartMessage(message, Color.RED)
            AutoRestart.instance.restartSent = true //declared in plugin main file. prevents the {message} from sending twice.
            scheduleRestartCommand()
        }
    }

    private fun scheduleRestartCommand(): Runnable {
        val runnable = Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), restartConfig.command)
        }
        Bukkit.getScheduler().runTask(AutoRestart.instance, runnable)
        return runnable
    }

    private fun sendWarningMessage(minutesLeft: Int) {
        val timeLeftMessage = when {
            minutesLeft > 1 -> "$minutesLeft minutes"
            minutesLeft == 1 -> "$minutesLeft minute"
            else -> "less than a minute"
        }
        val message = "Warning: The server will restart in $timeLeftMessage!"
        broadcastRestartMessage(message, Color.YELLOW)
    }

    private fun getFormattedTimeLeft(): String {
        return when {
            hoursLeft == 0L -> "$minutesLeft minute${if (minutesLeft > 1) "s" else ""}"
            else -> {
                val hoursText = "$hoursLeft hour${if (hoursLeft > 1) "s" else ""}"
                val minutesText = "$minutesLeft minute${if (minutesLeft > 1) "s" else ""}"
                "$hoursText and $minutesText"
            }
        }
    }

    private fun broadcastRestartMessage(message: String, color: String) {
        val formattedMessage = createColorMessage(message, color)
        Bukkit.broadcast(Component.text(formattedMessage))
        logger.info(getAnsiColorCode(color) + formattedMessage.substring(2) + getAnsiColorCode(Color.RESET))
    }

    private fun createColorMessage(message: String, color: String): String {
        val colorCode = when (color) {
            Color.GREEN -> "§a"
            Color.RED -> "§c"
            Color.YELLOW -> "§e"
            else -> "§f"
        }
        return "$colorCode$message"
    }

    private fun getAnsiColorCode(color: String): String {
        return when (color) {
            Color.GREEN -> "\u001B[32m"
            Color.RED -> "\u001B[31m"
            Color.YELLOW -> "\u001B[33m"
            Color.RESET -> "\u001B[0m"
            else -> ""
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val now = ZonedDateTime.now()
        val nextExecutionTime = getNextExecutionTime(now) ?: return

        val minutesLeft = Duration.between(now, nextExecutionTime).toMinutes()
        logger.info("isManualRestart?: ${AutoRestart.instance.isManualRestart}")

        when {
            AutoRestart.instance.isManualRestart -> {
                val delay = AutoRestart.instance.restartDelay
                val message = "An unplanned restart is scheduled in $delay minute${if (delay > 1) "s" else ""}. Please prepare."
                event.player.sendMessage(createColorMessage(message, Color.RED))
            }
            minutesLeft in 1..30 -> {
                val message = "The server is restarting in ${getFormattedTimeLeft()}! Please prepare."
                event.player.sendMessage(createColorMessage(message, Color.YELLOW))
            }
            minutesLeft > 30 -> {
                val message = "No imminent restart. Next restart is in ${getFormattedTimeLeft()}."
                event.player.sendMessage(createColorMessage(message, Color.GREEN))
            }
        }
    }

    fun sendUnplannedRestartMessageToPlayers() {
        AutoRestart.instance.isManualRestart = true
        AutoRestart.instance.isRestartActive = true
        val restartDelay = AutoRestart.instance.restartDelay

        object : BukkitRunnable() {
            var remainingTime = restartDelay

            override fun run() {
                if (!AutoRestart.instance.isRestartActive) {
                    cancel()
                    return
                }
                if (remainingTime > 0) {
                    val message = "The server is restarting in $remainingTime minute${if (remainingTime > 1) "s" else ""}! Please prepare."
                    Bukkit.getOnlinePlayers().forEach { player ->
                        sendActionBarMessage(player, message, 3, 20)
                    }
                    remainingTime--
                } else {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        sendActionBarMessage(player, "The server is restarting now!", 3, 20)
                    }
                    cancel()
                }
            }
        }.runTaskTimer(AutoRestart.instance, 0L, 60 * 20L)
    }

    private fun sendActionBarMessage(player: Player, message: String, durationSeconds: Long = 0, intervalTicks: Long = 0) {
        if (durationSeconds > 0 && intervalTicks > 0) {
            val durationTicks = durationSeconds * 20
            val endTime = System.currentTimeMillis() + durationSeconds * 1000

            object : BukkitRunnable() {
                override fun run() {
                    if (System.currentTimeMillis() < endTime) {
                        val actionBar = Component.text(message).color(TextColor.fromHexString("#FF0000"))
                        player.sendActionBar(actionBar)

                        Bukkit.getScheduler().runTaskLater(AutoRestart.instance, Runnable {
                            player.sendActionBar(Component.text(""))
                        }, durationTicks)
                    } else {
                        cancel()
                    }
                }
            }.runTaskTimer(AutoRestart.instance, 0L, intervalTicks)
        } else {
            val actionBar = Component.text(message).color(TextColor.fromHexString("#FF0000"))
            player.sendActionBar(actionBar)
        }
    }

    fun sendCancelledRestart() {
        val message = "The unplanned server restart has been cancelled. Continue as you were!"
        Bukkit.getOnlinePlayers().forEach { player ->
            sendActionBarMessage(player, message)
        }
        AutoRestart.instance.isManualRestart = false
    }

}

