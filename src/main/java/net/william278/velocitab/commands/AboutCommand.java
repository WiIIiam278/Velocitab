package net.william278.velocitab.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.plugin.Plugin;
import net.william278.desertwell.AboutMenu;
import net.william278.desertwell.Version;
import net.william278.velocitab.Velocitab;

public class AboutCommand implements SimpleCommand {
    private final AboutMenu aboutMenu;
    public AboutCommand(Velocitab plugin) {
        aboutMenu = AboutMenu.create("Velocitab")
                .withDescription("Velocitab is a super-simple Velocity TAB menu plugin that uses scoreboard team client-bound packets to actually sort player lists without the need for a backend plugin.")
                .withVersion(Version.fromString(plugin.getVersion(), "-"))
                .addAttribution("Author",
                        AboutMenu.Credit.of("William278").withDescription("Click to visit website").withUrl("https://william278.net"))
                .addAttribution("Contributors",
                        AboutMenu.Credit.of("Ironboundred").withDescription("Coding"),
                        AboutMenu.Credit.of("Emibergo02").withDescription("Coding"))
                .addButtons(
                        AboutMenu.Link.of("https://github.com/WiIIiam278/Velocitab/wiki").withText("Wiki").withIcon("⛏"),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").withText("Discord").withIcon("⭐").withColor("#6773f5"));
    }
    @Override
    public void execute(Invocation invocation) {
        invocation.source().sendMessage(aboutMenu.toMineDown().toComponent());
    }
}
