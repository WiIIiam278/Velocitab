Velocitab supports the full range of modern color formatting, including RGB colors and gradients. Both MiniMessage (_default_), MineDown and Legacy formatting are supported. To change which formatter is being used, change the `formatter` value in `config.yml` to `MINEDOWN`, `MINIMESSAGE` or `LEGACY` respectively.

Formatting is applied on header, footer and player text for each server group, and is applied after [[Placeholders]] have been inserted.


## MiniMessage syntax reference
MiniMessage is the default formatter type, enabled by setting `formatter` to `MINIMESSAGE` in `config.yml`. See the [MiniMessage Syntax Reference](https://docs.advntr.dev/minimessage/format.html) on the Adventure Docs for how to format text with it. Using MiniMessage as the formatter also allows compatibility for using MiniPlaceholders in text.

## MineDown syntax reference
MineDown formatting can be enabled by setting `formatter` to `MINEDOWN` in `config.yml`. See the [MineDown Syntax Reference](https://github.com/WiIIiam278/MineDown) on GitHub for the specification of how to format text with it.


## Legacy formatting
> **Warning:** The option for legacy formatting is provided only for backwards compatibility with other plugins. Please consider using the MineDown or MiniMessage options instead!

Legacy formatting can be enabled by setting `formatter` to `LEGACY` in `config.yml`. Legacy formatter supports Mojang color and formatting codes (e.g. `&d`, `&l`), Adventure-styled RGB color codes (e.g. `&#a25981`), as well as BungeeCord RGB color codes (e.g. `&x&a&2&5&9&8&1`). See the [LegacyComponentSerializer Syntax Reference](https://docs.advntr.dev/serializer/legacy.html) on the Adventure Docs for more technical details.

## Multi-line strings
In order to have a multi-line string in YAML, you can use the `|-` or `|` syntax. The `|-` syntax will remove last newline character, while the `|` syntax will keep it.
You can also use `\n` to add a newline character in a string.

### Example 1
```yaml
foo: |-
  bar 1
  bar 2
  bar 3
```

is equivalent to

```yaml
foo: "bar 1\nbar 2\nbar 3"
```

### Example 2

```yaml
foo: |
  bar 1
  bar 2
  bar 3
```

is equivalent to

```yaml
foo: "bar 1\nbar 2\nbar 3\n"
```

## List of multi lines strings

> **Note:** The examples above are generic examples on how yaml works in multi line. If you want to use multi line in headers & footers you need to provide a list of multi line strings like in the example below.

```yaml
    headers:
      - |
        <rainbow:!2>Running Velocitab by William278 & AlexDev03</rainbow>
        <gray>Second line of the first element</gray>
        <yellow>Third line of the first element</yellow>
      - |
        <rainbow:!4>Running Velocitab by William278 & AlexDev03</rainbow>
        <gray>Second line of the second element</gray>
        <yellow>Third line of the second element</yellow>
    footers:
      - <gray>There are currently %players_online%/%max_players_online% players online</gray>
      - | 
        <gray> Test 1 </gray>
        <yellow> Test 2 </yellow>
```

In this example the header will switch between the 2 elements, but it will always display all the 3 lines.

The footer in this example will switch between 2 elements, the first one is just a simple string, the second element will display 2 lines since it's a multi line string

<figure style="text-align: center;">
  <img src="https://i.imgur.com/YKu1RWi.gif" />
  <figcaption>Example of a header and footer with multi line strings</figcaption>
</figure>

