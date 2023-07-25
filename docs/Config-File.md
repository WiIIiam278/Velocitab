This page contains the configuration file reference for Velocitab. The config file is located in `/plugins/velocitab/config.yml`

## Example config
<details>
<summary>config.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃       Velocitab Config       ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┗╸ Placeholders: %players_online%, %max_players_online%, %local_players_online%, %current_date%, %current_time%, %username%, %server%, %ping%, %prefix%, %suffix%, %role%
# Header(s) to display above the TAB list for each server group.
# List multiple headers and set update_rate to the number of ticks between frames for basic animations
headers:
  default:
  - '&rainbow&Running Velocitab by William278'
# Footer(s) to display below the TAB list for each server group, same as headers.
footers:
  default:
  - '[There are currently %players_online%/%max_players_online% players online](gray)'
formats:
  default: '&7[%server%] &f%prefix%%username%'
# Which text formatter to use (MINEDOWN, MINIMESSAGE, or LEGACY)
formatting_type: MINEDOWN
# The servers in each group of servers
server_groups:
  default:
  - server
  - server2
# All servers which are not in other groups will be put in the fallback group.
# "false" will exclude them from Velocitab.
fallback_enabled: true
# The formats to use for the fallback group.
fallback_group: default
# Only show other players on a server that is part of the same server group as the player.
only_list_players_in_same_group: true
# Define custom names to be shown in the TAB list for specific server names.
# If no custom display name is provided for a server, its original name will be used.
server_display_names:
  very-long-server-name: VLSN
# Whether to enable the PAPIProxyBridge hook for PAPI support
enable_papi_hook: true
# How long in seconds to cache PAPI placeholders for, in milliseconds. (0 to disable)
papi_cache_time: 30000
# If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.
enable_miniplaceholders_hook: true
# Whether to sort players in the TAB list.
sort_players: true
# Ordered list of elements by which players should be sorted. (ROLE_WEIGHT, ROLE_NAME and SERVER_NAME are supported)
sort_players_by:
- ROLE_WEIGHT
- ROLE_NAME
# How often in milliseconds to periodically update the TAB list, including header and footer, for all users.
# If set to 0, TAB will be updated on player join/leave instead. (1s = 1000ms)
update_rate: 0
```

</details>

## Details
### Server Groups
Which formatting and the header/footer to use for a player's TAB list is determined by the group of servers they are currently connected to. See [[Server Groups]] for more information.

### Formatting
Velocitab supports the full range of modern color formatting, including RGB colors and gradients, through either MineDown or MiniMessage syntax. See [[Formatting]] for more information.

### Animations
Velocitab supports basic header and footer animations by adding multiple frames of animation and setting the update rate to a value greater than 0.

### Placeholders
You can use various placeholders that will be replaced with values (for example, `%username%`) in your config. Support for PlaceholderAPI is also available through [a bridge library plugin](https://modrinth.com/plugin/papiproxybridge), as is the component-based MiniPlaceholders for users of that plugin with the MiniMessage formatter. See [[Placeholders]] for more information.