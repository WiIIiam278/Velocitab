Velocitab supports formatting the nametags of players (the text displayed above their heads). This can be used to display a player's rank, group, or other information using placeholders. Please note some limitations apply.

![Nametags being updated by Velocitab in-game](https://raw.githubusercontent.com/WiIIiam278/Velocitab/master/images/nametags.png)

> **Note:** This feature requires sending Update Teams packets. `send_scoreboard_packets` must be enabled in the [`config.yml` file](config-file) for this to work. [More details...](sorting#compatibility-issues)

## Customizing nametags
You can configure nametags per-group using the `nametag` field in `tab_groups.yml`. Each group must have a nametag format associated with it, which will be applied to all players on servers in that group. Nametags are comprised of a prefix and suffix; the player's username will be displayed in-between.

<details>
<summary>Editing nametags (tab_groups.yml)</summary>

```yaml
nametag:
  prefix: '&f%prefix%'
  suffix: '&f%suffix%'
```
</details>

Only players on servers which are part of groups that specify nametag formats will have their nametag formatted. To disable nametag formatting, remove all groups from the `nametags` section of the config file (leaving it empty).

## Disabling nametags
If you don't want Velocitab to format player nametags, set `prefix` and `suffix` to an empty string in each tab group (e.g., `prefix: ''`). You should also set `remove_nametags` to `true` in the [`config.yml` file](config-file).

<details>
<summary>Remove nametags option (config.yml)</summary>

```yaml
remove_nametags: true
```
</details>

## Named pets
A feature of the game since Minecraft 1.9 is that pets given a nametag inherit their owner's team prefix/suffix/color. This is an intentional game feature, and not a bug. Since Velocitab uses team prefixes/colors for name tag formatting, pets will have their name formatted using their owner's prefix/suffix. A side effect of this, however, is that setting `remove_nametags` to `true` hides nametags on pets.

You can install the [PetNameFix](https://www.spigotmc.org/resources/petnamefix.109466/) plugin if you don't like this bit of Vanilla behaviour.

## Formatting limitations
Nametags must adhere to the following restrictions:
* Nametag prefixes and suffixes can contain full RGB formatting, but the color used in the player's name between the two (effectively, their "Scoreboard Team" color) is limited to the set of legacy color codes.
  * Velocitab determines which color to use here based on the last color format used in the configured prefix (displayed before their name), downsampled from RGB if necessary.
  * To control this, simply set the prefix format to end with a valid [team color](https://wiki.vg/Text_formatting#Colors) you want to use (e.g. `&4` for dark_red in Minedown formatting).
* Nametags cannot contain newlines (must be on a single line).
