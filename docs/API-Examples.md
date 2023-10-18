Velocitab provides an API for vanishing (hiding) and modifying the names of players as they appear in the TAB list for other players.

This page assumes you have read the general [[API]] introduction and that you have both imported Velocitab into your project and added it as a dependency.

## 1. Vanishing/Un-vanishing a player

### 1.1 Vanishing a player
Use `VelocitabAPI#vanishPlayer` to vanish a player. This method takes a Velocity `Player` as a parameter.

This will hide a user from all TAB lists (they will not be shown). Note this will not remove them at a packet level; Vanish plugins should use this API feature as a utility that forms part of their Vanish implementation.

<details>
<summary>Example &mdash; Vanishing a player</summary>

```java
// Vanishing a proxy Player
VelocitabAPI.vanishPlayer(player);
```
</details>

### 1.2 Un-vanishing a player
Use `VelocitabAPI#unvanishPlayer` to un-vanish a player. This method takes a Velocity `Player` as a parameter.

This will allow the user to be shown in all TAB lists again.

<details>
<summary>Example &mdash; Un-vanishing a player</summary>

```java
// Un-vanishing a proxy Player
VelocitabAPI.unvanishPlayer(player);
```
</details>

### 1.3 Providing a Vanish Integration
You can provide a Vanish integration to provide a managed class to Vanish/Unvanish a player through the `VelocitabAPI#setVanishIntegration` instance.

## 2. Modifying a player's name
You can set a custom name for a player that will be displayed in `%name%` placeholders in the TAB list. This can be used to display a player's nickname, for example. This is done through `VelocitabAPI#setCustomPlayerName()`, which accepts a Velocity `Player` and a `String` custom name

<details>
<summary>Example &mdash; Setting a custom name for a player</summary>

```java
// Setting a custom name for a proxy Player
VelocitabAPI.setCustomPlayerName(player, "CustomName");
```
</details>

You can also use `VelocitabAPI#getCustomPlayerName(Player)` to get a player's custom name, wrapped in an `Optional<String>` that will return with the String of the player's custom name if one has been set (otherwise `Optional#empty`)

<details>
<summary>Example &mdash; Getting a player's custom name</summary>

```java
// Getting a player's custom name
Optional<String> customName = VelocitabAPI.getCustomPlayerName(player);
```
</details>