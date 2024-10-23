This page will walk you through installing Velocitab on a Velocity proxy server.

## Requirements
* A Velocity proxy server (running Velocity 3.4.0 or newer)
* Backend Minecraft servers. The following Minecraft server versions are fully supported:
  - Minecraft 1.8&mdash;1.8.9
  - Minecraft 1.12.2&mdash;latest

&dagger;_Servers that support clients with versions not listed are bit supported, as Velocitab relies on dispatching protocol-compatible packets and modern 1.16 RGB chat color formatting. Users attempting to connect on earlier versions may cause errors to display in your proxy server console._

## Installation
1. Download the latest version of [Velocitab](https://modrinth.com/plugin/velocitab)
2. Drag the plugin into the `/plugins/` folder on your Velocity proxy server
3. Download and install additional optional dependencies on your proxy and backend servers as needed:
   1. If you'd like Velocitab to display user roles from [LuckPerms](https://luckperms.net/), ensure LuckPerms is installed on your Velocity proxy as well, and configured to synchronise role information over your database
   2. If you'd like to use [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholders, install [PAPIProxyBridge](https://modrinth.com/plugin/papiproxybridge) on both your Velocity proxy and Spigot-based Minecraft servers. Also ensure PlaceholderAPI is installed on your spigot servers
   3. If you'd like to use [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) placeholders, install MiniPlaceholders on your Velocity proxy.
4. Restart your proxy to let Velocitab generate its configuration file
5. Stop your proxy server, modify the [`config.yml`](config-file) file to your liking, and start your server

Velocitab should now be successfully installed on your proxy.

## Next Steps
* [Configuring Velocitab](config-file)
* [Using Placeholders](placeholders)
* [Text Formatting](formatting)
* [Server Groups](server-groups)
* [Using Animations](animations)
