<p align="center">
    <img src="images/banner.png" alt="HuskChat" />
    <a href="https://github.com/WiIIiam278/Velocitab/actions/workflows/java_ci.yml">
        <img src="https://img.shields.io/github/actions/workflow/status/WiIIiam278/Velocitab/java_ci.yml?branch=master&logo=github"/>
    </a>
    <a href="https://discord.gg/tVYhJfyDWG">
        <img src="https://img.shields.io/discord/818135932103557162.svg?label=&logo=discord&logoColor=fff&color=7389D8&labelColor=6A7EC2" />
    </a>
</p>
<br/>
A super-simple Velocity TAB menu plugin that uses scoreboard team client-bound packets to actually sort player lists without the need for a backend plugin.

## Installation
Requires [Protocolize](https://github.com/Exceptionflug/protocolize) v2.2.5 to be installed on your proxy. [LuckPerms](https://luckperms.net) is also strongly recommended for prefix/suffix/role (and sorting) support.

Simply download the latest release and place it in your Velocity plugins folder (along with Protocolize).

## Configuration
Velocitab has a simple config file that lets you define a header, footer and format for the player list, as well as a set of servers you do not want to have the custom player list appear on (i.e. if you want certain backend servers to manage the tab list instead of the proxy).

### Placeholders
You can include placeholders in the header, footer and player name format of the TAB list. The following placeholders are supported:

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

## License
Velocitab is licensed under the Apache 2.0 license.

- [License](https://github.com/WiIIiam278/Velocitab/blob/master/LICENSE)

---
&copy; [William278](https://william278.net/), 2023. Licensed under the Apache-2.0 License.