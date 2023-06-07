If you'd like to display a custom logo or image in your TAB menu, like the example listed on the project listing,<sup><a href="#ref-1">[1]</a></sup> you will need to make use of a resource pack to retexture Minecraft's default Unicode characters.

<details>
<summary>Example: <i>Mine in Abyss</i> server TAB menu.</summary>

!["Mine In Abyss" server TAB menu, featuring a custom logo](https://github.com/WiIIiam278/Velocitab/assets/31187453/de3f24a7-cdff-4575-9b3d-446fb77a75c4)
</details>

## Creating a resource pack
To do this, you'll need to make a resource pack, and set it to be used on your servers as the Server Resource Pack<sup><a href="#ref-2">[2]</a></sup>. To do this:
1. Create a blank resource pack. Consult the [Minecraft Wiki](https://minecraft.fandom.com/wiki/Resource_pack) for making a super basic resource pack layout; this involves creating a `pack.mcmeta` for the correct Minecraft version and placing this inside the root of a directory.
2. Create the directory to store your logo texture: `/assets/server_name/textures/font`. Replace `server_name` with your server name (lower case, no spaces)
3. Place your logo in that directory. The logo can have a maximum size of 256x256 (though you can technically use multiple characters if you want a larger logo; feel free to experiment)
4. Create the `/assets/minecraft/font` directory.
5. Create `default.json`, which we will use to specify a unicode character to replace with your logo texture.
6. Add the following to your `default.json` file, which will replace the (non-existent) Unicode character `\ue238` with your logo. Remember to replace `server_name` with the server name you used earlier:
```json
{
    "providers": [
        {
            "file": "server_name:font/logo.png",
            "chars": [
                "\ue238"
            ],
            "height": 50,
            "ascent": 35,
            "type": "bitmap"
        }
    ]
}
```
7. Save the file. Note that later on you may need to tweak the `height` and `ascent` values in the file to suit your logo's size.
8. For testing, install the resource pack locally by placing it in your `~/.minecraft/resourcepacks` folder and select your newly created pack in the Resource Packs menu.
9. Copy the Unicode character from [fileformat.info's unicode browsertest page](https://fileformat.info/info/unicode/char/e238/browsertest.htm) and paste it to chat in-game. Observe and verify that your logo looks correct.

## Applying your pack to your servers and Velocitab
10. In Velocitab, add your logo's Unicode character to the header section of one or more of your TAB menus. You may need to add multiple newlines (`\n`) after the Unicode character to add spacing between the header and player list. Use `/velocitab reload` to get it right.
11. Finally, set the pack as your server resource pack, by uploading it to a service that lets you supply a direct download link into your `server.properties` files. You should do this on all your backend (Spigot/Paper/Folia/Fabric, etc.) servers.
12. Restart everything and fine tune as necessary.

## The entire _Bee Movie_ as an animated TAB header
You totally could render the entire _Bee Movie_ in the TAB menu by retexturing a ton of impossible Unicode characters with this method, yes (at a low resolution, granted). Bare in mind there are limits on maximum server resource pack sizes, so you'd need to do some optimizations. Your Velocitab config file would also be very long. Have fun.

----
<a id="ref-1" />

[1] &mdash; Courtesy of <a href="https://mineinabyss.com">Mine In Abyss</a>. <i>Made in Abyss</i> © Akihito Tsukushi, Takeshobo / Made in Abyss Production Committee.

<a id="ref-2" />

[2] &mdash; Taken from [this helpful Reddit comment](https://www.reddit.com/r/admincraft/comments/llrgty/comment/gnswdcz/) on [/r/admincraft](https://www.reddit.com/r/admincraft/) by [/u/MrPowerGamerBR](https://www.reddit.com/user/MrPowerGamerBR/).
