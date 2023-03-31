package net.william278.velocitab.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.tab.PlayerTabList;

public class ReloadCommand implements SimpleCommand {
    private final Velocitab plugin;

    public ReloadCommand(Velocitab plugin) {
        this.plugin = plugin;
    }
    @Override
    public void execute(Invocation invocation) {
        plugin.loadSettings();
        plugin.getTabList().reloadUpdate();
        invocation.source().sendMessage(Component.text("Velocitab has been reloaded!").color(TextColor.color(255, 199, 31)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("velocitab.reload");
    }
}
