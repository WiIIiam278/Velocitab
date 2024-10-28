Velocitab provides a plugin message API, to let you do things with Velocitab from your backend servers.

> **Note:** This feature requires sending Update Teams packets. `send_scoreboard_packets` must be enabled in the [`config.yml` file](config-file) for this to work. [More details...](sorting#compatibility-issues)
> 
## Prerequisites
To use the Velocitab plugin message API, you must first turn it on and ensure the following:

* That `enable_plugin_message_api` and `send_scoreboard_packets` is set to `true` in your Velocitab [[config file]]
* That `bungee-plugin-message-channel` is set to `true` in your **Velocity proxy config** TOML (see [Velocity config reference](https://docs.papermc.io/velocity/configuration)).

## API Requests from Backend Plugins

### 1 Changing player's username in the TAB list
To change a player's username in the TAB list, you can send a plugin message on the channel `velocitab:update_custom_name` with a `customName` string, where `customName` is the new desired display name.
<details>
<summary>Example &mdash; Changing player's username in the TAB List</summary>

```java
player.sendPluginMessage(plugin, "velocitab:update_custom_name", "Steve".getBytes());
```
</details>

### 2 Update color of player's nametag
To change player's [nametag](nametags) color, you can send a plugin message on the channel `velocitab:update_team_color` with `teamColor` string, where `teamColor` is the new desired name tag color.

You can only use legacy color codes, for example `a` for green, `b` for aqua, etc. Please note this option overrides the color of the glow potion effect if set. [Check here](https://wiki.vg/index.php?title=Text_formatting&oldid=18983#Colors) for a list of supported colors (The value under the "Code" header on the table is what you need).

<details>
<summary>Example &mdash; Changing player's team color</summary>

```java
player.sendPluginMessage(plugin, "velocitab:update_team_color", "a".getBytes());
```
</details>
