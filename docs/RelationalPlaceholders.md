# Velocitab MiniPlaceholders Expansion Documentation

In order to use these placeholders, install MiniPlaceholders on your Velocity proxy, set the `formatter_type` to `MINIMESSAGE` and ensure `enable_miniplaceholders_hook` is set to `true`

In all examples target is the one that sees the message, and the audience is the one that is being seen.

Example:
My username is `William278` and I can see in tablist an audience player named `Player1`.

## Table of Placeholders

| Placeholder                                 | Description                                                                                                     | Example Usage |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------|---------------|
| `<velocitab_rel_who-is-seeing>`             | Displays the username of the target player, used for debug                                                      | `William278`  |
| `<velocitab_rel_perm:(permission):(value)>` | Checks if the target player has a specific permission and, if true parse value with the audience player's name. | See below     |
| `<velocitab_rel_vanish>`                    | Checks if the audience player can see the target player, considering the vanish status.                         | `true`        |

## Examples of `<velocitab_rel_perm:(permission):(value)>` Placeholder

**Note:** In the value, you can [Velocitab](Placeholders.md) placeholders or [MiniPlaceholders](https://github.com/MiniPlaceholders/MiniPlaceholders/wiki/Placeholders#proxy-expansion)

| Placeholder Example Usage                                  | Description                                                                                                   | Output Example           |
|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|--------------------------|
| `<velocitab_rel_perm:check.server:Target is on %server%!>` | Checks if the target player has the permission 'check.server' and displays the server of the audience player. | `Target is on survival!` |
| `<velocitab_rel_perm:clientcheck:<player_client>>`         | Checks if the target player has the permission 'clientcheck' and displays the client of the audience player.  | `LunarClient`            |
| `<velocitab_rel_perm:pingcheck:<player_ping>>`             | Checks if the target player has the permission 'pingcheck' and displays the ping of the audience player.      | `23`                     |
