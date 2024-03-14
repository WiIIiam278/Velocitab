This page contains configuration file references for Velocitab. 
The config file is located in `/plugins/velocitab/config.yml` and the tab groups file is located in `/plugins/velocitab/tab_groups.yml`

## Example config
<details>
<summary>config.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃       Velocitab Config       ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ Information: https://william278.net/project/velocitab
# ┗╸ Documentation: https://william278.net/docs/velocitab

# Check for updates on startup
check_for_updates: true
# Whether to remove nametag from players' heads if the nametag associated with their server group is empty.
remove_nametags: true
# Whether to disable header and footer if they are empty and let backend servers handle them.
disable_header_footer_if_empty: true
# Which text formatter to use (MINEDOWN, MINIMESSAGE, or LEGACY)
formatter: MINEDOWN
# All servers which are not in other groups will be put in the fallback group.
# "false" will exclude them from Velocitab.
fallback_enabled: true
# The formats to use for the fallback group.
fallback_group: default
# Define custom names to be shown in the TAB list for specific server names.
# If no custom display name is provided for a server, its original name will be used.
server_display_names:
  very-long-server-name: VLSN
# Whether to enable the PAPIProxyBridge hook for PAPI support
enable_papi_hook: true
# How long in seconds to cache PAPI placeholders for, in milliseconds. (0 to disable)
papi_cache_time: 30000
# If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.
enable_mini_placeholders_hook: true
# Whether to send scoreboard teams packets. Required for player list sorting and nametag formatting.
# Turn this off if you're using scoreboard teams on backend servers.
send_scoreboard_packets: true
# If built-in placeholders return a blank string, fallback to Placeholder API equivalents.
# For example, if %prefix% returns a blank string, use %luckperms_prefix%. Requires PAPIProxyBridge.
fallback_to_papi_if_placeholder_blank: false
# Whether to sort players in the TAB list.
sort_players: true
# Remove gamemode spectator effect for other players in the TAB list.
remove_spectator_effect: false
# Whether to enable the Plugin Message API (allows backend plugins to perform certain operations)
enable_plugin_message_api: true
```

</details>

## Example tab groups

<details>

<summary>tab_groups.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃      Velocitab TabGroups     ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ Information: https://william278.net/project/velocitab
# ┗╸ Documentation: https://william278.net/docs/velocitab

groups:
- name: default
  headers:
  - '&rainbow&Running Velocitab by William278'
  footers:
  - '[There are currently %players_online%/%max_players_online% players online](gray)'
  format: '&7[%server%] &f%prefix%%username%'
  nametag:
    prefix: '&f%prefix%'
    suffix: '&f%suffix%'
  servers:
  - ^lobby[^ ]*
  - survival
  - creative
  - minigames
  - skyblock
  - prison
  sorting_placeholders:
  - '%role_weight%'
  - '%username_lower%'
  collisions: false
  header_footer_update_rate: 1000
  placeholder_update_rate: 1000
  only_list_players_in_same_server: false
```

</details>

## Details
### Server Groups
Which formatting and the header/footer to use for a player's TAB list is determined by the group of servers they are currently connected to. See [[Server Groups]] for more information.

### Formatting
Velocitab supports the full range of modern color formatting, including RGB colors and gradients, through either MineDown or MiniMessage syntax. See [[Formatting]] for more information.

## Nametags
As well as updating the text in the TAB menu, Velocitab supports updating player nametags (the text displayed above their heads). See [[Nametags]] for more information.

### Animations
Velocitab supports basic header and footer animations by adding multiple frames of animation and setting the update rate to a value greater than 0.

### Placeholders
You can use various placeholders that will be replaced with values (for example, `%username%`) in your config. Support for PlaceholderAPI is also available through [a bridge library plugin](https://modrinth.com/plugin/papiproxybridge), as is the component-based MiniPlaceholders for users of that plugin with the MiniMessage formatter. See [[Placeholders]] for more information.