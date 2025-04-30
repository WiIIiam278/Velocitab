/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.william278.desertwell.about.AboutMenu;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.config.TabGroups;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class VelocitabCommand {

    // System locales
    private final String systemDumpConfirm = """
            <color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| Prepare a system dump? This will include:</color>
            <gray>• Your latest server logs and Velocitab config files</gray>
            <gray>• Current plugin system status information</gray>
            <gray>• Information about your Java & Minecraft server environment</gray>
            <gray>• A list of other currently installed plugins</gray>
            <click:run_command:/velocitab dump confirm><hover:show_text:'<gray>Click to prepare dump'><color:#00fb9a>To confirm, use: <italic>/velocitab dump confirm</italic></color></click>
            """;
    private final String systemDumpStarted = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| Preparing system status dump, please wait…</color>";
    private final String systemDumpReady = "<click:open_url:%url%><color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| System status dump prepared! Click to view:</color>\n<underlined><color:gray>%url%</color></underlined></click>";
    private final String systemUpToDate = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| You are running the latest version of Velocitab (v%ver%).</color>";
    private final String systemUpdateAvailable = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| A new version of HuskClaims is available: v%new% (running: v%ver%).</color>";
    private final String systemReloaded = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| Reloaded config and tab group files.</color>";

    // Command locales
    private final String tabNameUpdated = "<color:#00fb9a>Your TAB name has been updated!</color>";
    private final String tabNameReset = "<color:#00fb9a>Your TAB name has been reset.</color>";

    // Error locales
    private final String errorPlayerNotFound = "<color:#ff3300>Error:</color> <color:#ff7e5e>Could not find the player %name%.</color>";
    private final String errorTabNameChangeUntracked = "<color:#ff3300>Error:</color> <color:#ff7e5e>You cannot update your TAB name from an untracked server!</color>";
    private final String errorTabNameResetUntracked = "<color:#ff3300>Error:</color> <color:#ff7e5e>You cannot reset your TAB name from an untracked server!</color>";
    private final String errorTabNameResetUnchanged = "<color:#ff3300>Error:</color> <color:#ff7e5e>You do not have a custom TAB name!</color>";

    private final AboutMenu aboutMenu;
    private final Velocitab plugin;

    public VelocitabCommand(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.aboutMenu = AboutMenu.builder()
                .title(Component.text("Velocitab"))
                .description(Component.text(plugin.getDescription().getDescription().orElseThrow()))
                .version(plugin.getVersion())
                .credits("Authors",
                        AboutMenu.Credit.of("William278").description("Click to visit website").url("https://william278.net"),
                        AboutMenu.Credit.of("AlexDev03").description("Click to visit GitHub").url("https://github.com/alexdev03"))
                .credits("Contributors",
                        AboutMenu.Credit.of("Ironboundred").description("Code"),
                        AboutMenu.Credit.of("Emibergo02").description("Code"),
                        AboutMenu.Credit.of("FreeMonoid").description("Code"),
                        AboutMenu.Credit.of("4drian3d").description("Code"))
                .buttons(
                        AboutMenu.Link.of("https://william278.net/docs/velocitab").text("Docs").icon("⛏"),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").text("Discord").icon("⭐").color(TextColor.color(0x6773f5)),
                        AboutMenu.Link.of("https://modrinth.com/plugin/velocitab").text("Modrinth").icon("◎").color(TextColor.color(0x589143)))
                .build();
    }

    @NotNull
    public BrigadierCommand command() {
        final LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
                .<CommandSource>literal("velocitab")
                .executes(ctx -> {
                    sendAboutInfo(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("about")
                        .executes(ctx -> {
                            sendAboutInfo(ctx.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("name")
                        .requires(src -> hasPermission(src, "name"))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("name", StringArgumentType.greedyString())
                                .requires(src -> src instanceof Player)
                                .executes(ctx -> {
                                    final Player player = (Player) ctx.getSource();
                                    final String name = StringArgumentType.getString(ctx, "name");
                                    final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
                                    if (tabPlayer.isEmpty()) {
                                        ctx.getSource().sendRichMessage(errorTabNameChangeUntracked);
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    tabPlayer.get().setCustomName(name);
                                    plugin.getTabList().updateDisplayName(tabPlayer.get());

                                    ctx.getSource().sendRichMessage(tabNameUpdated);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .executes(ctx -> {
                            final Player player = (Player) ctx.getSource();
                            final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
                            if (tabPlayer.isEmpty()) {
                                ctx.getSource().sendRichMessage(errorTabNameResetUntracked);
                                return Command.SINGLE_SUCCESS;
                            }

                            // If no custom name is applied, ask for argument
                            String customName = tabPlayer.get().getCustomName().orElse("");
                            if (customName.isEmpty() || customName.equals(player.getUsername())) {
                                ctx.getSource().sendRichMessage(errorTabNameResetUnchanged);
                                return Command.SINGLE_SUCCESS;
                            }

                            tabPlayer.get().setCustomName(null);
                            plugin.getTabList().updateDisplayName(tabPlayer.get());
                            ctx.getSource().sendRichMessage(tabNameReset);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(src -> hasPermission(src, "reload"))
                        .executes(ctx -> {
                            final Settings settings = plugin.getSettings();
                            try {
                                plugin.loadSettings();
                            } catch (Throwable e) {
                                plugin.setSettings(settings);
                                ctx.getSource().sendRichMessage("<red>An error occurred while reloading the settings file");
                                plugin.log(Level.ERROR, "An error occurred while reloading the settings file", e);
                                return Command.SINGLE_SUCCESS;
                            }

                            final Map<String, Group> groups = Map.copyOf(plugin.getTabGroupsManager().getGroupsMap());
                            final Map<TabGroups, String> groupsFiles = Map.copyOf(plugin.getTabGroupsManager().getGroupsFilesMap());

                            try {
                                plugin.getTabGroupsManager().loadGroups();
                            } catch (Throwable e) {
                                plugin.getTabGroupsManager().loadGroupsBackup(groups, groupsFiles);
                                ctx.getSource().sendRichMessage("<red>An error occurred while reloading the tab groups file");
                                plugin.log(Level.ERROR, "An error occurred while reloading the tab groups file", e);
                                return Command.SINGLE_SUCCESS;
                            }

                            plugin.getTabList().reloadUpdate();
                            ctx.getSource().sendRichMessage(systemReloaded);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("debug")
                        .requires(src -> hasPermission(src, "debug"))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("tablist")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.string())
                                        .suggests((ctx, builder1) -> {
                                            final String input = builder1.getRemainingLowerCase();
                                            if (input.isEmpty()) {
                                                return builder1.buildFuture();
                                            }
                                            plugin.getServer().getAllPlayers().stream()
                                                    .map(Player::getUsername)
                                                    .filter(s -> s.toLowerCase().contains(input))
                                                    .forEach(builder1::suggest);
                                            return builder1.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            final String input = ctx.getArgument("player", String.class);
                                            final Optional<Player> player = plugin.getServer().getPlayer(input);
                                            if (player.isEmpty()) {
                                                ctx.getSource().sendRichMessage(errorPlayerNotFound
                                                        .replaceAll("%name%", input));
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            player.get().getTabList().getEntries().forEach(entry -> {
                                                final String name = entry.getProfile().getName();
                                                final UUID uuid = entry.getProfile().getId();
                                                final String unformattedDisplayName = entry.getDisplayNameComponent()
                                                        .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
                                                        .orElse("empty");

                                                ctx.getSource().sendMessage(Component.text(
                                                        "Name: %s, UUID: %s, Unformatted display name: %s"
                                                                .formatted(name, uuid, unformattedDisplayName)));
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        ))
                .then(LiteralArgumentBuilder.<CommandSource>literal("dump")
                        .requires(src -> hasPermission(src, "dump"))
                        .executes(ctx -> {
                            ctx.getSource().sendRichMessage(systemDumpConfirm.trim());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(LiteralArgumentBuilder.<CommandSource>literal("confirm")
                                .executes(ctx -> {
                                    ctx.getSource().sendRichMessage(systemDumpStarted);
                                    plugin.getServer().getScheduler().buildTask(plugin, () -> {
                                        final String url = plugin.createDump(ctx.getSource());
                                        ctx.getSource().sendRichMessage(systemDumpReady.replace("%url%", url));
                                    }).schedule();
                                    return Command.SINGLE_SUCCESS;
                                })
                        ))
                .then(LiteralArgumentBuilder.<CommandSource>literal("update")
                        .requires(src -> hasPermission(src, "update"))
                        .executes(ctx -> {
                            plugin.getUpdateChecker().check().thenAccept(checked -> {
                                if (checked.isUpToDate()) {
                                    ctx.getSource().sendRichMessage(systemUpToDate
                                            .replaceAll("%ver%", plugin.getVersion().toString()));
                                    return;
                                }
                                ctx.getSource().sendRichMessage(systemUpdateAvailable
                                        .replaceAll("%new%", checked.getLatestVersion().toString())
                                        .replaceAll("%ver%", plugin.getVersion().toString()));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );

        return new BrigadierCommand(builder);
    }

    private boolean hasPermission(@NotNull CommandSource source, @NotNull String command) {
        return source.hasPermission(String.join(".", "velocitab", "command", command));
    }

    private void sendAboutInfo(@NotNull CommandSource source) {
        source.sendMessage(aboutMenu.toComponent());
    }

}
