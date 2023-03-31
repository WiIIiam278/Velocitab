package net.william278.velocitab.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.AboutMenu;
import net.william278.desertwell.Version;
import net.william278.velocitab.Velocitab;

import java.util.List;

public class VelocitabCommand implements SimpleCommand {
    private final AboutMenu aboutMenu;
    private final Velocitab plugin;

    public VelocitabCommand(Velocitab plugin) {
        this.plugin = plugin;
        aboutMenu = AboutMenu.create("Velocitab")
                .withDescription("Velocitab is a super-simple Velocity TAB menu plugin that uses scoreboard team client-bound packets to actually sort player lists without the need for a backend plugin.")
                .withVersion(Version.fromString(plugin.getVersion(), "-"))
                .addAttribution("Author",
                        AboutMenu.Credit.of("William278").withDescription("Click to visit website").withUrl("https://william278.net"))
                .addAttribution("Contributors",
                        AboutMenu.Credit.of("Ironboundred").withDescription("Coding"),
                        AboutMenu.Credit.of("Emibergo02").withDescription("Coding"))
                .addButtons(
                        AboutMenu.Link.of("https://william278.net/docs/velocitab").withText(" Wiki").withIcon("⛏"),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").withText(" Discord").withIcon("⭐").withColor("#6773f5"),
                        AboutMenu.Link.of("https://modrinth.com/plugin/velocitab").withText(" Modrinth").withIcon("X").withColor("#589143"));
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length >= 1) {
             if (invocation.arguments()[0].equalsIgnoreCase("reload")) {
                 reloadSettings(invocation.source());
                 return;
             }
        }

        sendAboutInfo(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.source().hasPermission("velocitab.command.reload")) {
            return List.of("about", "reload");
        }else {
            return List.of("about");
        }
    }

    private void sendAboutInfo(CommandSource source) {
        source.sendMessage(aboutMenu.toMineDown().toComponent());
    }

    private void reloadSettings(CommandSource source) {
        if (source.hasPermission("velocitab.command.reload")) {
            plugin.loadSettings();
            plugin.getTabList().reloadUpdate();
            source.sendMessage(Component.text("Velocitab has been reloaded!").color(TextColor.color(255, 199, 31)));
        }else {
            source.sendMessage(Component.text("You do not have permission to use this command"));
        }
    }
}
