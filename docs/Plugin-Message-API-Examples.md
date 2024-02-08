Velocitab provides a plugin message API.

## API Requests from Backend Plugins

### 1 Changing player's username in the TAB List
To change a player's username in the tablist, you can send a plugin message with the channel `velocitab:main` and as data `UPDATE_CUSTOM_NAME:::customName`.
Remember to replace `customName` with the desired name.
<details>
<summary>Example &mdash; Changing player's username in tablist</summary>

```java
player.sendPluginMessage(plugin, "velocitab:main", "UPDATE_CUSTOM_NAME:::Steve".getBytes());
```
</details>

### 2 Update team color
To change a player's team color in the TAB List, you can send a plugin message with the channel `velocitab:main` and as data `UPDATE_TEAM_COLOR:::teamColor`.
You can only use legacy color codes, for example `a` for green, `b` for aqua, etc.
This option overrides the glow effect if set

<details>
<summary>Example &mdash; Changing player's team color</summary>

```java
player.sendPluginMessage(plugin, "velocitab:main", "UPDATE_TEAM_COLOR:::a".getBytes());
```
</details>