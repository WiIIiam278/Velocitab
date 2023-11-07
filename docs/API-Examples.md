Velocitab provides an API for vanishing (hiding) and modifying the names of players as they appear in the TAB list for other players.

This page assumes you have read the general [[API]] introduction and that you have both imported Velocitab into your project, added it as a dependency and having an instance of `VelocitabAPI` available. For the following examples, an instance called `velocitabAPI` was used.

## 1. Vanishing/Un-vanishing a player

### 1.1 Vanishing a player
Use `VelocitabAPI#vanishPlayer` to vanish a player. This method takes a Velocity `Player` as a parameter.

This will hide a user from all TAB lists (they will not be shown). Note this will remove them at a packet level; Vanish plugins should use this API feature as a utility that forms part of their Vanish implementation.
Be sure to not remove the entry from TabList with Velocity API or direct packet as the packet would be sent twice and could cause a client-side bug.
This won't send an EntityRemovePacket so your vanish plugin should send it. On a backend server you can just use Player#hidePlayer and Player#showPlayer.

<details>
<summary>Example &mdash; Vanishing a player</summary>

```java
// Vanishing a proxy Player
velocitabAPI.vanishPlayer(player);
```
</details>

### 1.2 Un-vanishing a player
Use `VelocitabAPI#unVanishPlayer` to un-vanish a player. This method takes a Velocity `Player` as a parameter.

This will allow the user to be shown in all TAB lists again.

<details>
<summary>Example &mdash; Un-vanishing a player</summary>

```java
// Un-vanishing a proxy Player
velocitabAPI.unVanishPlayer(player);
```
</details>

### 1.3 Providing a Vanish Integration
You can provide a Vanish integration to provide a managed class to Vanish/Unvanish a player through the `VelocitabAPI#setVanishIntegration` instance.

## 2. Modifying a player's name
You can set a custom name for a player that will be displayed in `%name%` placeholders in the TAB list. This can be used to display a player's nickname, for example. This is done through `VelocitabAPI#setCustomPlayerName`, which accepts a Velocity `Player` and a `String` custom name.
This won't change the player's name in nametags and name list when you press T (key to open chat) and then press tab.

<details>
<summary>Example &mdash; Setting a custom name for a player</summary>

```java
// Setting a custom name for a proxy Player
velocitabAPI.setCustomPlayerName(player, "CustomName");
```
</details>

You can also use `VelocitabAPI#getCustomPlayerName` which accepts a Velocity `Player`, to get player's custom name, wrapped in an `Optional<String>` that will return the String of the player's custom name if one has been set (otherwise `Optional#empty`)

<details>
<summary>Example &mdash; Getting a player's custom name</summary>

```java
// Getting a player's custom name
Optional<String> customName = velocitabAPI.getCustomPlayerName(player);
```
</details>

## 3. Listening to PlayerAddedToTabEvent
You can listen to `PlayerAddedToTabEvent` to get notified when a player is added to a group TabList.

<details>
<summary>Example &mdash; Listening to PlayerAddedToTabEvent</summary>

```java
@Subscribe
public void onPlayerAddedToTab(PlayerAddedToTabEvent event) {
    VelocitabAPI velocitabAPI = VelocitabAPI.getInstance();
    velocitabAPI.setCustomPlayerName(event.player().getPlayer(), "CustomName");
}
```

</details>