
Velocitab supports placeholder replacements, which allow you to replace a placeholder with a different value. This is useful for things like changing the text of a date placeholder to a localized version, changing the text of a biome placeholder to a color or you can use a vanish placeholder to show a player's vanish status if the placeholder returns just a boolean (true/false).


## Configuring

Placeholder replacements are configured in the `placeholder_replacements` section of the every Tab Group. 
You can specify a list of replacements for a placeholder, and the replacements will be applied in the order they are listed.

The replacements are specified as a list of objects with two properties: `placeholder` and `replacement`.
`placeholder` is the placeholder to replace, and `replacement` is the replacement text.

### Example section
```yaml
placeholder_replacements:
  '%current_date_weekday_en-US%':
    - placeholder: Monday
      replacement: <red>Monday</red>
    - placeholder: Tuesday
      replacement: <gold>Tuesday</gold>
    - placeholder: Else
      replacement: <green>Other day</green>
  '%player_world_type%':
    - placeholder: Overworld
      replacement: '<aqua>Overworld</aqua>'
    - placeholder: Nether
      replacement: '<red>Nether</red>'
    - placeholder: End
      replacement: '<yellow>End</yellow>'
  '%player_biome%':
    - placeholder: PLAINS
      replacement: <red>Plains</red>
    - placeholder: DESERT
      replacement: <yellow>Desert</yellow>
    - placeholder: RIVER
      replacement: <aqua>River</aqua>
```

## Specified cases

### Vanish status
If you want to show a player's vanish status, for example, you can use the `%advancedvanish_is_vanished%` placeholder. 
This placeholder returns a boolean value, so you can use it to show a player's vanish status.

For example, if you wanted to show a player's vanish status as a color, you could use the following replacements:
```yaml
placeholder_replacements:
  '%advancedvanish_is_vanished%':
    - placeholder: Yes
      replacement: <red>Vanished</red>
    - placeholder: No
      replacement: <green>Not vanished</green>
```

### Else clause
If you don't want to specify every possible value for a placeholder, you can use the `ELSE` placeholder. 
This placeholder will be replaced with the replacement text of the first replacement that doesn't have a placeholder.

For example, if you wanted to show the current date as a color, you could use the following replacements:
```yaml
placeholder_replacements:
  '%current_date_weekday_en-US%':
    - placeholder: Monday
      replacement: <red>Monday</red>
    - placeholder: Tuesday
      replacement: <gold>Tuesday</gold>
    - placeholder: ELSE
      replacement: <green>Other day</green>
```

### Placeholder not present in a server
If you have a group with multiple servers, and you have a placeholder that is not present in one of the servers, you can use the `%<placeholder>%` as a placeholder it will handle the case where the placeholder is not present in the server.

```yaml
placeholder_replacements:
  '%huskhomes_homes_count%':
    - placeholder: '%huskhomes_homes_count%'
      replacement: <red>No homes in this server</red>
```

If you want you can also set the replacement as an empty string, which will be replaced with the empty string.

```yaml
placeholder_replacements:
  '%huskhomes_homes_count%':
    - placeholder: '%huskhomes_homes_count%'
      replacement: ''
```