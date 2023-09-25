Velocitab can sort players in the TAB list by a number of "sorting elements." Sorting is enabled by default, and can be disabled with the `sort_players` option in the [`config.yml`](Config-File) file.

## Sortable elements
To modify what players are sorted by, modify the `sorting_placeholders` list in the [`config.yml`](Config-File) file. This option accepts an ordered list; the first element in the list is what players will be sorted by first, with subsequent elements being used to break ties. The default sorting strategy is to sort first by `%role_weight%` followed by `%username%`.

<details>
<summary>Sort Players By&hellip; (config.yml)</summary>

```yaml
# Ordered list of elements by which players should be sorted. (Correct values are both internal placeholders and (if enabled) PAPI placeholders)
sort_players_by:
  - %role_weight%
  - %username%
```
</details>

### List of elements
The following sorting elements are supported:

|     Sorting element     | Description                                                                                  |
|:-----------------------:|----------------------------------------------------------------------------------------------|
| `Internal Placeholders` | [Check docs here](https://william278.net/docs/velocitab/placeholders#default-placeholders)   |
|   `PAPI Placeholders`   | [Check docs here](https://william278.net/docs/velocitab/placeholders#placeholderapi-support) |

## Technical details
In Minecraft, the TAB list is sorted by the client; the server does not handle the actual display order of names in the list. Players are sorted first by the name of their scoreboard team, then by their name. This is why having a proxy TAB plugin sort players is a surprisingly complex feature request!

To get the client to correctly sort the TAB list, Velocitab sends fake scoreboard "Update Teams" packets to everyone in order to trick the client into thinking players on the server are members of a fake scoreboard team. The name of the fake team for sorting is based on a number of "sorting elements," which can be customized.

Velocitab has a few optimizations in place to reduce the number of packets sent; if you update frequently sorting element placeholders, do note this will lead to more packets being sent between clients and the proxy as the teams will need to be updated more regularly. This can lead to an observable increase in network traffic&mdash;listing fewer sorting elements in the `sort_players_by` section will reduce the number of packets sent.


## Compatibility issues
There are a few compatibility caveats to bear in mind with sorting players in the TAB list:

* If you're using scoreboard teams on your server, then this will interfere with Velocitab's fake team packets and cause sorting to break. Most modern Minecraft proxy network servers probably won't use this feature, since there are better and more powerful plugin alternatives for teaming players, but it's still important to bear in mind.
* Some mods can interfere with scoreboard team packets, particularly if they internally deal with managing packets or scoreboard teams.
* Sending fake scoreboard team packets might not work correctly on some Minecraft server implementations such as [Quilt](https://quiltmc.org/).

In these cases, you may need to disable sorting through the `sort_players` option detailed earlier.