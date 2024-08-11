In order to use these placeholders, install MiniPlaceholders on your Velocity proxy, set the `formatter_type`
to `MINIMESSAGE`, and ensure `enable_miniplaceholders_hook` is set to `true`.

Conditional placeholders allow you to display different values based on certain conditions. The format
is `<velocitab_rel_condition|<condition>|<true>|<false>>`.

Currently, this system is only available for the `format` and `nametag` fields in the tab groups configuration.

## Table of Conditional Placeholders

| Placeholder Example                                                                                                | Description                                                                                                                         | Example Output                                         |
|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `<velocitab_rel_condition:%vault_eco_balance% >= 10:rich:poor>`                                                    | Checks if the player's vault balance is greater than or equal to 10. If true, displays "rich", else "poor".                         | `rich` or `poor`                                       |
| `<velocitab_rel_condition:%player_health% < 10:Low Health:Healthy>`                                                | Checks if the player's health is below 10. If true, displays "Low Health", else "Healthy".                                          | `Low Health` or `Healthy`                              |
| `<velocitab_rel_condition:%player_ping% <= 50:Good Ping:Bad Ping>`                                                 | Checks if the player's ping is 50 or below. If true, displays "Good Ping", else "Bad Ping".                                         | `Good Ping` or `Bad Ping`                              |
| `<velocitab_rel_condition:%player_level% >= 30:High Level:Low Level>`                                              | Checks if the player's level is 30 or above. If true, displays "High Level", else "Low Level".                                      | `High Level` or `Low Level`                            |
| `<velocitab_rel_condition:%player_exp% >= 1000:XP Master:XP Novice>`                                               | Checks if the player has 1000 or more experience points. If true, displays "XP Master", else "XP Novice".                           | `XP Master` or `XP Novice`                             |
| `<velocitab_rel_condition:"%player_name%" == ''AlexDev_'' OR "%player_name%" == ''William278_'':VelocitabDev:>`    | Checks if the player's name is either "AlexDev_" or "William278". If true, displays "Developer", else "NotDev".                     | `Developer` or `NotDev`                                |                       
| `<velocitab_rel_condition:startsWith(''%player_name%'', ''AlexDe''):IsAlex:NotAlex>`                               | Checks if the player's name starts with "AlexDe". If true, displays "IsAlex", else "NotAlex".                                       | `IsAlex` or `NotAlex`                                  |
| `<velocitab_rel_condition:endsWith(''%player_name%'', ''278''):EndsWith278:DoesNotEndWith278>`                     | Checks if the player's name ends with "278". If true, displays "EndsWith278", else "DoesNotEndWith278".                             | `EndsWith278` or `DoesNotEndWith278`                   |
| `<velocitab_rel_condition:"%player_gamemode%" == ''CREATIVE'':Creative Mode:Not Creative Mode>`                    | Checks if the player is in creative mode. If true, displays "Creative Mode", else "Not Creative Mode".                              | `Creative Mode` or `Not Creative Mode`                 |
| `<velocitab_rel_condition:"%player_world%" == ''nether'':In Nether:Not in Nether>`                                 | Checks if the player is in the Nether. If true, displays "In Nether", else "Not in Nether".                                         | `In Nether` or `Not in Nether`                         |
| `<velocitab_rel_condition:"%player_biome%" == "DESERT":In Desert:Not in Desert>`                                   | Checks if the player is in a desert biome. If true, displays "In Desert", else "Not in Desert".                                     | `In Desert` or `Not in Desert`                         |
| `<velocitab_rel_condition:''%player_gamemode%''.contains(''S''):Survival or Spectator:Not Survival or Spectator> ` | Checks if the player is in survival or spectator mode. If true, displays "Survival or Spectator", else "Not Survival or Spectator". | `Survival or Spectator` or `Not Survival or Spectator` |
| `<velocitab_rel_condition:%player_health% == %target_player_health%:Same health:Not same health> `                 | Checks if the player's health is the same as the target player's health. If true, displays "Same health", else "Not same health".   | `Same health` or `Not same health`                     |

**Note:** For string comparisons, use double quotes `" "` or single quotes `' '`. For numerical comparisons, quotes are
not needed.
Also if you use `'` for quotes, you need to escape them with `''`. The same applies for `"` and `""`. Example: `''%player_name%''` or `"'%player_name%'"`
In order to use papi placeholders for target you need to use `''%target_player_name%''` in order to get `''%player_name%''` replaced with the target player's name.

If you want to use `:` as a character in the condition or in the true/false value, you need to replace it with `?dp?`. Example: `<velocitab_rel_condition:%player_health% == %target_player_health%:Value?dp?True:Value?dp?False>`.

# Example
If you want to compare audience player's health with target player's health, you can use the following configuration:
```yaml
format: "<velocitab_rel_condition:%player_health% == %target_player_health%:Same health:Not same health>"
```

This is system is based on [JEXL](https://commons.apache.org/proper/commons-jexl/reference/examples.html) expressions.