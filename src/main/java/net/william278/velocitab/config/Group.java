package net.william278.velocitab.config;

import java.util.List;

public record Group(
        List<String> headers,
        List<String> footers,
        String format,
        String nametag,
        List<String> servers
        ) {
}
