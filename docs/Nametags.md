Velocitab supports formatting the nametags of players (the text displayed above their heads). This can be used to display a player's rank, group, or other information using placeholders. Please note some limitations apply.

![Nametags being updated by Velocitab in-game](https://raw.githubusercontent.com/WiIIiam278/Velocitab/master/images/nametags.png)

> **Note:** This feature requires sending Update Teams packets. `send_scoreboard_packets` must be enabled in the [`config.yml` file](config-file) for this to work. [More details...](sorting#compatibility-issues)

## Setting name tags
You can configure nametags per-group using the `nametags` section of the config file. Each group should have one nametag format associated with it, which will be applied to all players on servers in that group.

<details>
<summary>Editing nametags (config.yml)</summary>

```yaml
# Nametag(s) to display above players' heads for each server group. Set to empty to disable.
# Nametag formats must contain a %username%. Docs: https://william278.net/docs/velocitab/nametags
nametags:
  default: '&f%prefix%%username%&f%suffix%'

# (...)

# Whether to send scoreboard teams packets. Required for player list sorting and nametag formatting.
# Turn this off if you're using scoreboard teams on backend servers.
send_scoreboard_packets: true
```
</details>

Only players on servers which are part of groups that specify nametag formats will have their nametag formatted. To disable nametag formatting, remove all groups from the `nametags` section of the config file (leaving it empty).

## Removing name tags
In order to remove nametags, you must remove your nametag format from the config file. If you want to remove nametags for all groups, you can set the `nametags` section to empty `nametags: {}`. After that be sure to set `remove_nametags` to `true` to make sure the nametags are removed from players.

## Formatting limitations
Nametags must adhere to the following restrictions:
* A %username% placeholder must be present. This is used for delimiting the scoreboard prefix, name, and suffix to facilitate formatting.
* Only legacy colors can be used in formats. If RGB colors are specified, they will automatically be downsampled to the nearest legacy color. This is a limitation of the scoreboard team system.
* Nametags cannot contain newlines (must be on a single line)
* Gradients are not supported.