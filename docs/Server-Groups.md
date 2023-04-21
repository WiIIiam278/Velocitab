Velocitab supports defining multiple server groups, each providing distinct formatting for players in the TAB list, alongside unique headers and footers. This is useful if you wish to display different information in TAB depending on the server a player is on.

## Defining groups
Groups are defined in the `server_groups` section of `config.yml`, as a list of servers following the group name (by default, a group `default` will be present, alongside a list of servers on your network.

<details>
<summary>Example of a default config.yml</summary>

```yaml
server_groups:
  default:
    - lobby1
    - lobby2
    - lobby3
```
</details>

You can define as many groups as you wish in this section by adding more lists of servers.

<details>
<summary>Adding more groups</summary>

```yaml
server_groups:
  lobbies:
    - lobby1
    - lobby2
  creative:
    - creative_lobby
    - creative1
  survival:
    - survival1
    - survival2
```
</details>

## Mapping headers, footers & player formats to groups
Once you've defined your groups, you can modify the `headers`, `footers` and `formats` section of the file with different formats for each group.

<details>
<summary>Per-group formats</summary>

```yaml
headers:
  lobbies: 
   - 'Welcome, %username%! Join a server to start!'
  creative: 
   - '%username% is playing Creative!'
  survival: 
   - '%username% is playing Survival!'
footers:
  lobbies: 
   - 'There are %players_online%players online!'
  creative: 
   - 'Currently connected to a creative server: %server%!'
  survival: 
   - 'Today is %current_date%!'
formats:
  lobbies: '&8[Lobby] &7%username%'
  creative: '&e[Creative] &7[%server%] &f%prefix%%username%'
  survival: '&2[Survival (%server%)] &f%prefix%%username%'
```
</details>

See [[Placeholders]] for how to use placeholders in these formats, and [[Formatting]] for how to format text with colors, and see [[Animations]] for how to create basic animations by adding more headers/footers to each group's list.

### Adding new lines
If you want to add a new line to your header or footer format, you can use `\n` to insert one &mdash; but since this gets messy quickly, there's an easier way using the YAML markup pipe character to declare a multiline string:

<details>
<summary>Multi-line headers/footers</summary>

```yaml
footers:
  lobbies: 
   - |
     There are %players_online%players online!
     I'm a second line
     Third line, woohoo~!
```
</details>

Player name formats may only utilize one line.

## Default group
If a player isn't connected to a server on your network, their TAB menu will be formatted as per the formats defined by `fallback_group` set in `config.yml`, provided `fallback_enabled` is set to `true`.

If you don't want them to have their TAB handled at all by Velocitab, you can use this to disable Velocitab formatting on certain servers altogether by disabling the `fallback_enabled` setting and excluding servers you do not wish to format from being part of a group.

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