Velocitab lets you create basic animations in the header and footer, which you can combine with some nice [[Formatting]] to create a slick TAB menu for your server. Note you cannot animate player name formats, only headers and footers.

## Creating basic animations
By default, Velocitab headers/footers are static; only containing a single frame of animation and only updating when a user joins/leaves your server, or when permissions are recalcualated by LuckPerms.

### Adding additional frames of animation
To add additional frames of animation to a header format for a [server group](server-groups), add it to the string list for that group. The example below uses the MineDown gradient fade feature to create a simple three-phase animation.

<details>
<summary>Basic header animation (config.yml)</summary>

```yaml
headers:
  - '<rainbow>Running Velocitab by William278 & AlexDev03</rainbow>'
  - '<rainbow:10>Running Velocitab by William278 & AlexDev03</rainbow>'
  - '<rainbow:20>Running Velocitab by William278 & AlexDev03</rainbow>'
```
</details>

### Setting the frame rate
The `header_footer_update_rate` setting in your `tab_groups.yml` (different for each group) file&mdash;set to `0` by default&mdash;controls the length (in milliseconds&dagger;) between your TAB list being updated. On each update, the header or footer format will use the next frame in the list, looping back to the first after the last one has been displayed. 

A good starting value for this could be `1000`, which is equivalent to one second. Once you've changed the value, use `/velocitab reload` to update the TAB menu in-game without restarting your proxy. Note the minimum update rate is `200` to avoid excessive network packet traffic, so values between `1`-`199` will be rounded up to `200`. If this value is set to `0` or below (as it is by default), the TAB menu will only update when a player joins or leaves, permissions are recalculated on LuckPerms, or the proxy is reloaded.

&dagger;`1ms = 1/1000th` of a second.

## Example: Rainbow fade
![Example rainbow fade animation GIF](https://user-images.githubusercontent.com/31187453/232607366-35d530dc-fb2a-419b-a345-3cc758baa6df.gif)

Wondering how to make something like the above example? Here's how! This example uses MineDown formatting and its' gradient fade feature to create a convincing rainbow fade that's pleasing on the eyes.

<details>
<summary>Example rainbow fade (config.yml)</summary>

Please note this is not a complete tab_groups file; you will need to add the relevant sections to the correct part in your own Velocitab `tab_groups.yml`. 
```yaml
headers:
  - '<rainbow>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:1>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:2>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:3>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:4>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:5>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:6>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:7>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:8>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:9>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:10>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:11>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:12>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:13>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:14>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:15>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:16>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:17>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:18>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:19>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:20>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:21>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:22>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:23>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:24>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:25>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:26>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:27>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:28>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:29>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
  - '<rainbow:30>Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin</rainbow>'
footers:
  - |
    \n<gray>For Velocity proxy servers:</gray>
    <gradient:#1bd96a:#6cffa9>https://modrinth.com/plugin/velocitab</gradient>
    <gradient:#1bd96a:#6cffa9>https://william278.net/project/veloictab</gradient>'
format: '<gradient:#999:#fff>[%server%] &f%username%</gradient>'
header_footer_update_rate: 200
```
In config.yml
```yaml
formatter: MINIMESSAGE
```
</details>
