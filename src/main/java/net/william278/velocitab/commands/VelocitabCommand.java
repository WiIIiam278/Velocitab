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
                        AboutMenu.Link.of("https://modrinth.com/plugin/velocitab").withText("Modrinth").withIcon("X").withColor("#589143"));
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
