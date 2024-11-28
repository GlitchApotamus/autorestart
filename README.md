# AutoRestart Plugin for Minecraft Bukkit (Paper)

## Overview

The **AutoRestart Plugin** is a Minecraft server plugin designed to automate server restarts based on a customizable schedule. It sends out warnings to players prior to the restart and allows for manual restarts as well. The plugin works by reading a cron-style schedule from its configuration and handles both automated and unplanned restarts efficiently.

## Features

- **Automatic Server Restarts**: Configure the server to restart based on a cron schedule.
- **Customizable Warning Intervals**: Players receive warnings as the restart time approaches.
- **Manual Restarts**: Trigger server restarts manually and notify players in advance.
- **Action Bar Messages**: Players are notified with action bar messages about the upcoming restart.
- **Server Configuration**: Adjust restart time, delay, and command directly from the configuration file.

## Requirements

- **Minecraft Server Version**: 1.21+
- **Java Version**: Java 21 or higher
- **Plugin Dependencies**: None (this is a standalone plugin)

## Installation

1. **Download the Plugin**: Download the latest version of the AutoRestart plugin from the release page or build it from source.
2. **Place the Plugin**: Copy the `AutoRestart.jar` file into the `plugins/` folder of your Minecraft server.
3. **Start the Server**: Run or restart your Minecraft server to load the plugin.
4. **Configure the Plugin**: Edit the `config.toml` file located in `plugins/AutoRestart/` to adjust the restart settings.

## Configuration

### `config.toml`

The plugin is configured using the `config.toml` file, which is located in the plugin's folder (`plugins/AutoRestart/config.toml`).

```toml
[restart]
# Cron expression for scheduling automatic restarts (e.g., "0 0 3 * * *" for every 3 AM)
cron = "0 3 * * *"
# Command to execute when the server restarts (e.g., "stop" to stop the server)
command = "stop"
# List of warning intervals (in minutes) before the restart
warningIntervals = [30, 20, 15, 10, 5, 3, 1]
```