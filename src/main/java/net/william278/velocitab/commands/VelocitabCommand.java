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
import net.william278.desertwell.AboutMenu;
import net.william278.desertwell.Version;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

public final class VelocitabCommand {
    private final AboutMenu aboutMenu;
    private final Velocitab plugin;

    public VelocitabCommand(final @NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.aboutMenu = AboutMenu.create("Velocitab")
                .withDescription(plugin.getDescription().getDescription().orElseThrow())
                .withVersion(Version.fromString(plugin.getDescription().getVersion().orElseThrow(), "-"))
                .addAttribution("Author",
                        AboutMenu.Credit.of("William278").withDescription("Click to visit website").withUrl("https://william278.net"))
                .addAttribution("Contributors",
                        AboutMenu.Credit.of("Ironboundred").withDescription("Coding"),
                        AboutMenu.Credit.of("Emibergo02").withDescription("Coding"))
                .addButtons(
                        AboutMenu.Link.of("https://william278.net/docs/velocitab").withText("Docs").withIcon("⛏"),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").withText("Discord").withIcon("⭐").withColor("#6773f5"),
                        AboutMenu.Link.of("https://modrinth.com/plugin/velocitab").withText("Modrinth").withIcon("◎").withColor("#589143"));
    }

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
                                        TextColor.color(255, 199, 31)));
                                return Command.SINGLE_SUCCESS;
                            })
                    );

        return new BrigadierCommand(builder);
    }

    private void sendAboutInfo(CommandSource source) {
        source.sendMessage(aboutMenu.toMineDown().toComponent());
    }
}
