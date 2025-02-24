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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.william278.desertwell.about.AboutMenu;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public final class VelocitabCommand {

    private static final TextColor MAIN_COLOR = TextColor.color(0x00FB9A);
    private static final TextColor ERROR_COLOR = TextColor.color(0xFF7E5E);

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
                                        ctx.getSource().sendMessage(Component
                                                .text("You can't update your TAB name from an untracked server!", ERROR_COLOR));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    tabPlayer.get().setCustomName(name);
                                    plugin.getTabList().updateDisplayName(tabPlayer.get());

                                    ctx.getSource().sendMessage(Component
                                            .text("Your TAB name has been updated!", MAIN_COLOR));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .executes(ctx -> {
                            final Player player = (Player) ctx.getSource();
                            final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
                            if (tabPlayer.isEmpty()) {
                                ctx.getSource().sendMessage(Component
                                        .text("You can't reset your TAB name from an untracked server!", ERROR_COLOR));
                                return Command.SINGLE_SUCCESS;
                            }

                            // If no custom name is applied, ask for argument
                            String customName = tabPlayer.get().getCustomName().orElse("");
                            if (customName.isEmpty() || customName.equals(player.getUsername())) {
                                ctx.getSource().sendMessage(Component
                                        .text("You aren't using a custom name in TAB!", ERROR_COLOR));
                                return Command.SINGLE_SUCCESS;
                            }

                            tabPlayer.get().setCustomName(null);
                            plugin.getTabList().updateDisplayName(tabPlayer.get());
                            player.sendMessage(Component.text("Your name has been reset!", MAIN_COLOR));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(src -> hasPermission(src, "reload"))
                        .executes(ctx -> {
                            plugin.loadConfigs();
                            plugin.getTabList().reloadUpdate();
                            ctx.getSource().sendMessage(Component.text("Velocitab has been reloaded!",
                                    MAIN_COLOR));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                //debug
                .then(LiteralArgumentBuilder.<CommandSource>literal("debug")
                        .requires(src -> hasPermission(src, "debug"))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("tablist")
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.string())
                                        .suggests((ctx, builder1) -> {
                                            final String input = ctx.getInput();
                                            if (input.isEmpty()) {
                                                return builder1.buildFuture();
                                            }
                                            plugin.getServer().getAllPlayers().stream()
                                                    .map(Player::getUsername)
                                                    .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                                                    .forEach(builder1::suggest);
                                            return builder1.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            final String input = ctx.getArgument("player", String.class);
                                            final Optional<Player> player = plugin.getServer().getPlayer(input);
                                            if (player.isEmpty()) {
                                                ctx.getSource().sendMessage(Component.text("Player not found!", ERROR_COLOR));
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            player.get().getTabList().getEntries().forEach(entry -> {
                                                final String name = entry.getProfile().getName();
                                                final UUID uuid = entry.getProfile().getId();
                                                final String unformattedDisplayName = entry.getDisplayNameComponent().map(c -> PlainTextComponentSerializer.plainText().serialize(c)).orElse("empty");

                                                ctx.getSource().sendMessage(Component.text("Name: %s, UUID: %s, Unformatted display name: %s".formatted(name, uuid, unformattedDisplayName)));
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        ))
                //dump
                .then(LiteralArgumentBuilder.<CommandSource>literal("dump")
                        .requires(src -> hasPermission(src, "dump"))
                        .executes(ctx -> {
                            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                                final String dumpUrl = plugin.createDump(ctx.getSource());
                                final Component dumpUrlComponent = ctx.getSource() instanceof Player
                                        ? Component.text("Click to open dump: ", MAIN_COLOR).clickEvent(ClickEvent.openUrl(dumpUrl))
                                        : Component.text("Dump URL: " + dumpUrl, MAIN_COLOR);
                                ctx.getSource().sendMessage(dumpUrlComponent);
                            }).schedule();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("update")
                        .requires(src -> hasPermission(src, "update"))
                        .executes(ctx -> {
                            plugin.getUpdateChecker().check().thenAccept(checked -> {
                                if (checked.isUpToDate()) {
                                    ctx.getSource().sendMessage(Component.text("Velocitab is up to date! (Running v%s)"
                                            .formatted(plugin.getVersion()), MAIN_COLOR));
                                    return;
                                }
                                ctx.getSource().sendMessage(Component
                                        .text("An update for Velocitab is available. Please update to %s"
                                                .formatted(checked.getLatestVersion()), MAIN_COLOR));
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
