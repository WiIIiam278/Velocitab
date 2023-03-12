package net.william278.velocitab.config;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Different formatting markup options for the TAB list
 */
@SuppressWarnings("unused")
public enum Formatter {
    MINEDOWN((text, player, plugin) -> new MineDown(text).toComponent(),
            (text) -> text.replace("__", "_\\_")),
    MINIMESSAGE((text, player, plugin) -> plugin.getMiniPlaceholdersHook()
            .map(hook -> hook.format(text, player.getPlayer()))
            .orElse(MiniMessage.miniMessage().deserialize(text)),
            (text) -> MiniMessage.miniMessage().escapeTags(text));

    /**
     * Function to apply formatting to a string
     */
    private final TriFunction<String, TabPlayer, Velocitab, Component> formatter;
    /**
     * Function to escape formatting characters in a string
     */
    private final Function<String, String> escaper;

    Formatter(@NotNull TriFunction<String, TabPlayer, Velocitab, Component> formatter, @NotNull Function<String, String> escaper) {
        this.formatter = formatter;
        this.escaper = escaper;
    }

    @NotNull
    public Component format(@NotNull String text, @NotNull TabPlayer player, @NotNull Velocitab plugin) {
        return formatter.apply(text, player, plugin);
    }

    @NotNull
    public String escape(@NotNull String text) {
        return escaper.apply(text);
    }

}
