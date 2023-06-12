Velocitab supports a number of Placeholders that will be replaced with their respective proper values in-game. In addition to the set of provided default Placeholders, you can make use of PlaceholderAPI and MiniPlaceholder-provided placeholders through special hooks.

## Default placeholders
Placeholders can be included in the header, footer and player name format of the TAB list. The following placeholders are supported out of the box:

| Placeholder              | Description                                       | Example            |
|--------------------------|---------------------------------------------------|--------------------|
| `%players_online%`       | Players online on the proxy                       | `6`                |
| `%max_players_online%`   | Player capacity of the proxy                      | `500`              |
| `%local_players_online%` | Players online on the server the player is on     | `3`                |
| `%current_date%`         | Current real-world date of the server             | `24 Feb 2023`      |
| `%current_time%`         | Current real-world time of the server             | `21:45:32`         |
| `%username%`             | The player's username                             | `William278`       |
| `%server%`               | Name of the server the player is on               | `alpha`            |
| `%ping%`                 | Ping of the player (in ms)                        | `6`                |
| `%prefix%`               | The player's prefix (from LuckPerms)              | `&4[Admin]`        |
| `%suffix%`               | The player's suffix (from LuckPerms)              | `&c `              |
| `%role%`                 | The player's primary LuckPerms group name         | `admin`            |
| `%role_display_name%`    | The player's primary LuckPerms group display name | `Admin`            |
| `%debug_team_name%`      | Internal team value, used for list sorting        | `1_alpha_William2` |

### Customising server display names
You can make use of the `server_display_names` feature in `config.yml` to customise how server display name appear when using the `%server%` placeholder. In the below example, if a user is connected to a server with the name "`very-long-server-`name" and the player name format for the group that server belongs to includes a `%server%` placeholder, the placeholder would be replaced with "`VSLN`" instead of the full server name.

<details>
<summary>Server display names (config.yml)</summary>

```yaml
# Define custom names to be shown in the TAB list for specific server names.
# If no custom display name is provided for a server, its original name will be used.
server_display_names:
  very-long-server-name: VLSN
```
</details>

## PlaceholderAPI support
To use PlaceholderAPI placeholders in Velocitab, install the [PAPIProxyBridge](https://modrinth.com/plugin/papiproxybridge) library plugin on your Velocity proxy and all Minecraft spigot servers on your network, and ensure the  PAPI hook option is enabled in your Velocitab [[Config File]]. You can then include PAPI placeholders in your formats as you would any of the default placeholders.

## MiniPlaceholders support
If you are using MiniMessage [[Formatting]], you can use [MiniPlaceholders](https://github.com/MiniPlaceholders/MiniPlaceholders) with Velocitab for MiniMessage-styled component placeholders provided by other proxy plugins. Install MiniPlaceholders on your Velocity proxy, set the `formatter_type` to `MINIMESSAGE` and ensure `enable_miniplaceholders_hook` is set to `true`