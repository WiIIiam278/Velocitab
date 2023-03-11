package net.william278.velocitab.hook;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

public class MiniPlaceholdersHook extends Hook {

    public MiniPlaceholdersHook(@NotNull Velocitab plugin) {
        super(plugin);
    }

    @NotNull
    public Component format(@NotNull String text, @NotNull Audience player) {
        return MiniMessage.miniMessage().deserialize(text, MiniPlaceholders.getAudienceGlobalPlaceholders(player));
    }

}
