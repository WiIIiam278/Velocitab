package net.william278.velocitab.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import org.jetbrains.annotations.NotNull;

@YamlFile(header = "Velocitab Config File")
public class Settings {

    @YamlKey("tab_list.header")
    private String header = "Welcome to the server!";

    @YamlKey("tab_list.footer")
    private String footer = "Welcome to the server!";

    @YamlKey("tab_list.format")
    private String format;

    @YamlComment("Use LuckPerms for tab list formatting (if installed)")
    @YamlKey("use_luckperms")
    private boolean useLuckPerms = true;

    private Settings() {
    }

    @NotNull
    public String getHeader() {
        return header;
    }

    @NotNull
    public String getFooter() {
        return footer;
    }

    @NotNull
    public String getFormat() {
        return format;
    }

    public boolean isUseLuckPerms() {
        return useLuckPerms;
    }

}
