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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.about.AboutMenu;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

public final class VelocitabCommand {
    private static final TextColor MAIN_COLOR = TextColor.color(0x00FB9A);
    private final AboutMenu aboutMenu;
    private final Velocitab plugin;

    public VelocitabCommand(final @NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.aboutMenu = AboutMenu.builder()
                .title(Component.text("Velocitab"))
                .description(Component.text(plugin.getDescription().getDescription().orElseThrow()))
                .version(plugin.getVersion())
                .credits("Author",
                        AboutMenu.Credit.of("William278").description("Click to visit website").url("https://william278.net"))
                .credits("Contributors",
                        AboutMenu.Credit.of("Ironboundred").description("Coding"),
                        AboutMenu.Credit.of("Emibergo02").description("Coding"),
                        AboutMenu.Credit.of("FreeMonoid").description("Coding"),
                        AboutMenu.Credit.of("4drian3d").description("Coding"))
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
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(src -> src.hasPermission("velocitab.command.reload"))
                        .executes(ctx -> {
                            plugin.loadSettings();
                            plugin.getTabList().reloadUpdate();
                            ctx.getSource().sendMessage(Component.text(
                                    "Velocitab has been reloaded!",
                                    MAIN_COLOR));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("update")
                        .requires(src -> src.hasPermission("velocitab.command.update"))
                        .executes(ctx -> {
                            plugin.getUpdateChecker().check().thenAccept(checked -> {
                                if (checked.isUpToDate()) {
                                    ctx.getSource().sendMessage(Component
                                            .text("Velocitab is up to date! (Running v" + plugin.getVersion() + ")", MAIN_COLOR));
                                    return;
                                }
                                ctx.getSource().sendMessage(Component
                                        .text("An update for velocitab is available. " +
                                              "Please update to " + checked.getLatestVersion(), MAIN_COLOR));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );

        return new BrigadierCommand(builder);
    }

    private void sendAboutInfo(@NotNull CommandSource source) {
        source.sendMessage(aboutMenu.toComponent());
    }
}
