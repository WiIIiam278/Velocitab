<!--suppress ALL -->
<h4>
<p align="center">
    <img src="images/banner.png" alt="Velocitab" />
    <a href="https://modrinth.com/plugin/velocitab">
        <img src="https://img.shields.io/modrinth/v/velocitab?color=%231bd96a&label=modrinth&logo=modrinth&logoColor=%23fffff" />
    </a>
    <a href="https://github.com/WiIIiam278/Velocitab/actions/workflows/java_ci.yml">
        <img src="https://img.shields.io/github/actions/workflow/status/WiIIiam278/Velocitab/java_ci.yml?branch=master&logo=github"/>
    </a>
    <a href="https://discord.gg/tVYhJfyDWG">
        <img src="https://img.shields.io/discord/818135932103557162.svg?label=&logo=discord&logoColor=fff&color=7389D8&labelColor=6A7EC2" />
    </a>
</p>
<br/>
</h4>

**Velocitab** is a super-simple Velocity TAB menu plugin that uses scoreboard team client-bound packets to actually sort player lists without the need for a backend plugin.

![showcase.png](images/showcase.png)

## Setup
Requires [Protocolize](https://www.spigotmc.org/resources/protocolize-protocollib-for-bungeecord-waterfall-velocity.63778/) v2.2.5 to be installed on your proxy. [LuckPerms](https://luckperms.net) is also strongly recommended for prefix/suffix/role (and sorting) support.

Simply download the latest release and place it in your Velocity plugins folder (along with Protocolize).

## Configuration
Velocitab has a simple config file that lets you define a header, footer and format for the player list. You can additionally configure [groups of servers](https://william278.net/docs/velocitab/server-groups) to display different formats in the TAB menu depending on which server the player is viewing it from.

### Formatting
Velocitab [supports](https://william278.net/docs/velocitab/formatting) the full range of RGB colors and gradients, with options to use either MineDown (_default_), MiniMessage, or legacy formatting.

### Placeholders
You can include [placeholders](https://william278.net/docs/velocitab/placeholders) in the header, footer and player name format of the TAB list. The following placeholders are supported:

| Placeholder              | Description                                   | Example            |
|--------------------------|-----------------------------------------------|--------------------|
| `%players_online%`       | Players online on the proxy                   | `6`                |
| `%max_players_online%`   | Player capacity of the proxy                  | `500`              |
| `%local_players_online%` | Players online on the server the player is on | `3`                |
| `%current_date%`         | Current real-world date of the server         | `24 Feb 2023`      |
| `%current_time%`         | Current real-world time of the server         | `21:45:32`         |
| `%username%`             | The player's username                         | `William278`       |
| `%server%`               | Name of the server the player is on           | `alpha`            |
| `%ping%`                 | Ping of the player (in ms)                    | `6`                |
| `%prefix%`               | The player's prefix (from LuckPerms)          | `&4[Admin]`        |
| `%suffix%`               | The player's suffix (from LuckPerms)          | `&c `              |
| `%role%`                 | The player's primary LuckPerms group          | `admin`            |
| `%debug_team_name%`      | Internal team value, used for list sorting    | `1_alpha_William2` |

PlaceholderAPI placeholders are also supported. To use them, just install [PAPIProxyBridge](https://modrinth.com/plugin/papiproxybridge) on your Velocity proxy and backend Spigot servers. Additionally, a hook for MiniPlaceholders is supported for servers using the MiniMessage formatter.

### Command
You can use the `/velocitab reload` command to reload the plugin config file (permission: `velocitab.command.reload`) 

## Building
To build Velocitab, simply run the following in the root of the repository:
```bash
./gradlew clean build
```
The build will be output as `/target/Velocitab-x.xx.jar`.

### License
Velocitab is licensed under the Apache 2.0 license.

- [License](https://github.com/WiIIiam278/Velocitab/blob/master/LICENSE)

## Links
* **[Website](https://william278.net/project/velocitab)** — Visit my website!
* **[Docs](https://william278.net/docs/velocitab)** — Read the plugin docs!
* **[Discord](https://discord.com/invite/tVYhJfyDWG)** — Get support, ask questions!
* **[GitHub](https://github.com/WiIIiam278/Velocitab)** — Check out the plugin source code!

---
&copy; [William278](https://william278.net/), 2023. Licensed under the Apache-2.0 License.
