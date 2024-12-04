Velocitab supports a number of Placeholders that will be replaced with their respective proper values in-game. In addition to the set of provided default Placeholders, you can make use of PlaceholderAPI and MiniPlaceholder-provided placeholders through special hooks.

## Default placeholders
Placeholders can be included in the header, footer and player name format of the TAB list. The following placeholders are supported out of the box:

| Placeholder                     | Description                                                                                                                                       | Example            |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| `%players_online%`              | Players online on the proxy                                                                                                                       | `6`                |
| `%max_players_online%`          | Player capacity of the proxy                                                                                                                      | `500`              |
| `%local_players_online%`        | Players online on the server the player is on                                                                                                     | `3`                |
| `%group_players_online_(name)%` | Players online on the group provided                                                                                                              | `11`               |
| `%group_players_online%`        | Players online on player's group                                                                                                                  | `15`               |
| `%current_date_day%`            | Current day of the month                                                                                                                          | `14`               |
| `%current_date_weekday%`        | Current day of the week                                                                                                                           | `Wednesday`        |
| `%current_date_weekday_(tag)%`  | Current day of the week ([localized](https://en.wikipedia.org/wiki/IETF_language_tag#List_of_common_primary_language_subtags)) `it-IT` as example | `Mercoledì`        |
| `%current_date_month%`          | Current month of the year                                                                                                                         | `06`               |
| `%current_date_year%`           | Current year                                                                                                                                      | `2024`             |
| `%current_date%`                | Current real-world date of the server                                                                                                             | `14/06/2023`       |
| `%current_date_(tag)%`          | Current real-world date ([localized](https://en.wikipedia.org/wiki/IETF_language_tag#List_of_common_primary_language_subtags)) `en-US` as example | `06/14/2023`       |
| `%current_time_hour%`           | Current hour of the day                                                                                                                           | `21`               |
| `%current_time_minute%`         | Current minute of the hour                                                                                                                        | `45`               |
| `%current_time_second%`         | Current second of the minute                                                                                                                      | `32`               |
| `%current_time%`                | Current real-world time of the server                                                                                                             | `21:45:32`         |
| `%current_time_(tag)%`          | Current real-world time ([localized](https://en.wikipedia.org/wiki/IETF_language_tag#List_of_common_primary_language_subtags)) `en-US` as example | `9:45 PM`          |
| `%username%`                    | The player's username                                                                                                                             | `William278`       |
| `%username_lower%`              | The player's username, in lowercase                                                                                                               | `william278`       |
| `%server%`                      | Name of the server the player is on                                                                                                               | `alpha`            |
| `%ping%`                        | Ping of the player (in ms)                                                                                                                        | `6`                |
| `%prefix%`                      | The player's prefix (from LuckPerms)                                                                                                              | `&4[Admin]`        |
| `%suffix%`                      | The player's suffix (from LuckPerms)                                                                                                              | `&c `              |
| `%role%`                        | The player's primary LuckPerms group name                                                                                                         | `admin`            |
| `%role_display_name%`           | The player's primary LuckPerms group display name                                                                                                 | `Admin`            |
| `%role_weight%`                 | Comparable-formatted primary LuckPerms group weight                                                                                               | `100`              |
| `%luckperms_meta_(key)%`        | Formats a meta key from the user's LuckPerms group                                                                                                | (varies)           |
| `%server_group%`                | The name of the server group the player is on                                                                                                     | `default`          |
| `%server_group_index%`          | Indexed order of the server group in the list                                                                                                     | `0`                |
| `%debug_team_name%`             | (Debug) Player's team name, used for [[Sorting]]                                                                                                  | `1alphaWilliam278` |

**Note:** `(tag)` stands for IETF language tag, used for localization of date and time placeholders. For example, `en-US` for American English, `fr-FR` for French, `it-IT` for Italian, etc.
You can find a list of common primary language subtags [here](https://en.wikipedia.org/wiki/IETF_language_tag#List_of_common_primary_language_subtags).


## PlaceholderAPI support
To use PlaceholderAPI placeholders in Velocitab, install the [PAPIProxyBridge](https://modrinth.com/plugin/papiproxybridge) library plugin on your Velocity proxy and all Minecraft spigot servers on your network, and ensure the PAPI hook option is enabled in your Velocitab [[Config File]]. You can then include PAPI placeholders in your formats as you would any of the default placeholders.

PlaceholderAPI placeholders are cached to reduce plugin message traffic. By default, placeholders are cached for 30 seconds (30000 milliseconds); if you wish to use PAPI placeholders that update more frequently, you can reduce the cache time in the Velocitab config.yml file by adjusting the `papi_cache_time` value.

## MiniPlaceholders support
If you are using MiniMessage [[Formatting]], you can use [MiniPlaceholders](https://github.com/MiniPlaceholders/MiniPlaceholders) with Velocitab for MiniMessage-styled component placeholders provided by other proxy plugins. Install MiniPlaceholders on your Velocity proxy, set the `formatter_type` to `MINIMESSAGE` and ensure `enable_miniplaceholders_hook` is set to `true`
You can also use [Relational Placeholders](Relational-Placeholders).