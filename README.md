# AutoRestart Plugin for Minecraft Bukkit (Paper)

### The latest version of this plugin can always be found [HERE](https://ci.cc-haven.net/autorestart/latest)

## Overview

The **AutoRestart Plugin** is a Minecraft server plugin designed to automate server restarts based on a customizable schedule. 
It sends out warnings to players prior to the restart and allows for manual restarts as well. 
The plugin works by reading a cron-style schedule from its configuration and handles both automated and unplanned restarts efficiently.

## Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Automatic Server Restarts**: Configure the server to restart based on a cron schedule.
- **Customizable Warning Intervals**: Players receive warnings as the restart time approaches.
- **Manual Restarts**: Trigger server restarts manually and notify players in advance.
- **Action Bar Messages**: Players are notified with action bar messages about the upcoming restart.
- **Server Configuration**: Adjust restart time, warning intervals, and command directly from the configuration file.
- **Deploy Task**: Created a task to automate copying the plugin to the server plugin directory [Deploy](#Instructions)

## Requirements

- **Minecraft Server Version**: 1.21+
- **Java Version**: Java 21 or higher
- **Plugin Dependencies**: None (this is a standalone plugin)

## Installation

### Method 1: Download the Plugin

1. **Download the Plugin**:  
   Download the latest version of the AutoRestart plugin from the [release page](https://github.com/GlitchApotamus/autorestart/releases).

2. **Place the Plugin**:  
   Copy the `autorestart-all.jar` file into the `plugins/` folder of your Minecraft server.

3. **Start the Server**:  
   Run or restart your Minecraft server to load the plugin.

4. **Configure the Plugin**:  
   Edit the `config.toml` file located in `plugins/AutoRestart/` to adjust the restart settings.

---

### Method 2: Build the Plugin from Source

1. **Clone the Repository**:  
   Clone the repository to your local machine:
   ```bash
   git clone https://github.com/GlitchApotamus/AutoRestart.git
   cd path/to/autorestart
   # windows users
   gradlew clean build --refresh-dependencies
   gradlew clean build
   # linux/macos
   ./gradlew clean build --refresh-dependencies
   ./gradlew clean build
   ```
2. **Place the Plugin**:  
   Copy the `autorestart-all.jar` file from `autorestart/build/libs`  
   into the `plugins/` folder of your Minecraft server.

3. **Start the Server**:  
   Run or restart your Minecraft server to load the plugin.

4. **Configure the Plugin**:  
   Edit the `config.toml` file located in `plugins/AutoRestart/` to adjust the restart settings. 
   

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

# Usage
1. Simply drag and drop the plugin into your server and start it
2. Everything is automated after filling out the toml file
3. I also have (3) commands in the project: `reload`, `restart`, and `cancel`.  
All require `op` permissions.
   - reload: This will reload the configuration file if you change it while server is running.
   - restart: This will initiate a manual restart with the given time. 
      - currently, you should add an extra minute to the delay to function right.
      - this will be fixed in an update
   - cancel: Use this command cancel the manual restart immediately.
     - this is the only function of this command.

# Deploy

## Instructions
1. Create a file in the root of the project named "upload.sh"
2. copy and paste next line to the script
3. scp ./build/libs/autorestart-all.jar path/to/server/plugins/autorestart-all.jar
4. edit "path/to/server" to match the proper directory
5. if you need it on a server with ssh, use "username@ip:/path/to/server"
### Windows
    gradlew deploy
### Linux/MacOS
    ./gradlew deploy


# License
- This currently is free and open to use. Just please give credit where it's due!

# Credits
- [DuroCodes](https://github.com/DuroCodes) for helping me get the project set up
- [Minecraft Development](https://plugins.jetbrains.com/plugin/8327-minecraft-development) for making plugin development fast and easy

## Contributing

We welcome contributions! To contribute, please fork the repository and submit a pull request. Ensure that your changes are well-documented, and write tests where applicable.

For issues, feel free to open a new issue or discuss any problems with the team.
