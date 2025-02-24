Velocitab supports defining multiple server groups, each providing distinct formatting for players in the TAB list,
alongside unique headers and footers. This is useful if you wish to display different information in TAB depending on
the server a player is on. You can also set formatting to use for [[Nametags]] above players' heads per-group.

## Defining groups

Groups are defined in `tab_groups.yml`, as a list of TabGroup elements.

You can also add more tab groups by creating a folder called `tab_groups` in the `plugins/velocitab` folder, and adding
a `other_tab_groups.yml` (you can use a custom name, it needs to be .yml) file to it.

Every group must have a unique name, and a list of servers to include in the group. You can also define a list of
sorting placeholders to use when sorting players in the TAB list, and a header/footer update rate and placeholder update
rate to use for the group.

## Headers and footers

<details>
<summary>Example of headers and footers</summary>

```yaml
  headers:
    - '<rainbow>Running Velocitab by William278 & AlexDev03</rainbow>'
  footers:
    - '<gray>There are currently %players_online%/%max_players_online% players online</gray>'
```

</details>

You can define a list of headers and footers to use for each group. These will be cycled through at the rate defined
by `header_footer_update_rate` in milliseconds. If you only want to use one header/footer, you can define a single
element list. You can also use the `|` character to define a multi-line header/footer. See [[Animations]] for more
information.

## Formats

<details>
<summary>Example of format</summary>

```yaml
  format: '<gray>[%server%] %prefix%%username%</gray>'
```

</details>

You can define a format to use for each group. This will be used to format the text of each player in the TAB list.
See [[Formatting]] for more information.
Player formats may only utilize one line.

## Nametags

<details>
<summary>Example of nametag</summary>

```yaml
  nametag:
    prefix: '<white>%prefix%</white>'
    suffix: '<white>%suffix%</white>'
```

</details>

You can define a nametag to use for each group. This will be used to format the text above each player's head.
See [[Nametags]] for more information.
Player nametags may only utilize one line.

## Servers

<details>
<summary>Example of servers</summary>

```yaml
  servers:
    - lobby
    - survival
    - creative
    - minigames
    - skyblock
    - prison
    - hub
```

</details>

You can define a list of servers to include in each group.
The use of regex patterns is also valid since v1.6.4

<details>
<summary>Example regex pattern</summary>

```yaml
servers:
  - ^lobby-\d+$
```
This will include all servers starting with `lobby-` and ending with any integer

</details>

## Sorting placeholders

<details>
<summary>Example of sorting placeholders</summary>

```yaml
  sorting_placeholders:
    - '%role_weight%'
    - '%username_lower%'
```

</details>

You can define a list of sorting placeholders to use when sorting players in the TAB list. See [[Sorting]] for more
information.

## Header/footer update rate

<details>
<summary>Example of header/footer update rate</summary>

```yaml
  header_footer_update_rate: 1000
```

</details>

You can define a header/footer update rate to use for each group, in milliseconds. This will determine how quickly the
headers and footers will cycle through in the TAB list. The default is 1000 milliseconds (1 second).

## Format update rate

<details>
<summary>Example of format update rate</summary>

```yaml
  format_update_rate: 1000
```

</details>

You can define a format update rate to use for each group, in milliseconds. This will determine how quickly the
formats will update in the TAB list.

## Nametag update rate (sorting)

<details>
<summary>Example of nametag update rate</summary>

```yaml
  nametag_update_rate: 1000
```

</details>

You can define a nametag update rate to use for each group, in milliseconds. This will determine how quickly the
nametags will update in the TAB list. This will also determine how quickly the sorting will update.

## Placeholder update rate

<details>
<summary>Example of placeholder update rate</summary>

```yaml
  placeholder_update_rate: 1000
```

</details>

You can define a placeholder update rate to use for each group, in milliseconds. This will determine how quickly the
placeholders in the TAB list will update. The default is 1000 milliseconds (1 second).


## Example tab groups

<details>

<summary>Adding more groups</summary>

```yaml
groups:
  - name: lobbies
    headers:
      - '<rainbow:!2>Running Velocitab by William278 & AlexDev03 on Lobbies!</rainbow>'
    footers:
      - '<gray>There are currently %players_online%/%max_players_online% players online</gray>'
    format: '<gray>[%server%] %prefix%%username%</gray>'
    servers:
      - lobby
      - hub
      - minigames
      - creative
      - survival
    sorting_placeholders:
      - '%role_weight%'
      - '%username_lower%'
    header_footer_update_rate: 1000
    placeholder_update_rate: 1000
  - name: creative
    headers:
      - '<rainbow:!2>Running Velocitab by William278 & AlexDev03 on Creative!</rainbow>'
    footers:
      - '<gray>There are currently %players_online%/%max_players_online% players online</gray>'
    format: '<gray>[%server%] %prefix%%username%</gray>'
    servers:
      - creative
    sorting_placeholders:
      - '%role_weight%'
      - '%username_lower%'
    header_footer_update_rate: 1000
    format_update_rate: 1000
    nametag_update_rate: 1000
    placeholder_update_rate: 1000
  - name: survival
    headers:
      - '<rainbow:!2>Running Velocitab by William278 & AlexDev03 on Survival!</rainbow>'
    footers:
      - '<gray>There are currently %players_online%/%max_players_online% players online</gray>'
    format: '<gray>[%server%] %prefix%%username%</gray>'
    servers:
      - survival
    sorting_placeholders:
      - '%role_weight%'
      - '%username_lower%'
    header_footer_update_rate: 1000
    format_update_rate: 1000
    nametag_update_rate: 1000
    placeholder_update_rate: 1000
  ```

</details>

See [[Placeholders]] for how to use placeholders in these formats, and [[Formatting]] for how to format text with
colors, and see [[Animations]] for how to create basic animations by adding more headers/footers to each group's list.
Note that some formatting limitations apply to nametags &mdash; [[Nametags]] for more information.

## Default group

If a player isn't connected to a server on your network, their TAB menu will be formatted as per the formats defined
by `fallback_group` set in `config.yml`, provided `fallback_enabled` is set to `true`.

If you don't want them to have their TAB handled at all by Velocitab, you can use this to disable Velocitab formatting
on certain servers altogether by disabling the `fallback_enabled` setting and excluding servers you do not wish to
format from being part of a group.

<details>
<summary>Example in config.yml</summary>

```yaml
# All servers which are not in other groups will be put in the fallback group.
# "false" will exclude them from Velocitab.
fallback_enabled: true
# The formats to use for the fallback group.
fallback_group: 'lobbies'
```

</details>
