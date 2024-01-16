Velocitab lets you create basic animations in the header and footer, which you can combine with some nice [[Formatting]] to create a slick TAB menu for your server. Note you cannot animate player name formats, only headers and footers.

## Creating basic animations
By default, Velocitab headers/footers are static; only containing a single frame of animation and only updating when a user joins/leaves your server, or when permissions are recalcualated by LuckPerms.

### Adding additional frames of animation
To add additional frames of animation to a header format for a [server group](server-groups), add it to the string list for that group. The example below uses the MineDown gradient fade feature to create a simple three-phase animation.

<details>
<summary>Basic header animation (config.yml)</summary>

```yaml
headers:
  - '&rainbow&Running Velocitab by William278'
  - '&rainbow:10&Running Velocitab by William278'
  - '&rainbow:20&Running Velocitab by William278'
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
  - '&rainbow&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:2&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:4&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:6&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:8&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:10&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:12&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:14&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:16&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:18&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:20&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:22&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:24&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:26&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:28&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:30&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:32&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:34&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:36&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:38&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:40&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:42&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:44&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:46&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:48&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:50&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:52&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:54&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:56&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:58&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
  - '&rainbow:60&Velocitab ⭐ A super-simple (sorted!) Velocity TAB menu plugin\n'
footers:
  - |
    \n&7For Velocity proxy servers:
    &#1bd96a-#6cffa9&https://modrinth.com/plugin/velocitab
    &#1bd96a-#6cffa9&https://william278.net/project/veloictab'
format: '&#999-#fff&[%server%] &f%username%'
header_footer_update_rate: 200
```
In config.yml
```yaml
formatter: MINEDOWN
```
</details>
