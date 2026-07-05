package de.brentspine.mcstatus4j.responses;

import java.util.Arrays;
import java.util.List;

/** Software (brand/plugins) information from a GameSpy4 Query response. */
public record QuerySoftware(String version, String brand, List<String> plugins) {

  static QuerySoftware build(String version, String plugins) {
    ParsedPlugins parsed = parsePlugins(plugins);
    return new QuerySoftware(version, parsed.brand(), parsed.plugins());
  }

  private record ParsedPlugins(String brand, List<String> plugins) {}

  /**
   * Parse the raw {@code "Brand: plugin1; plugin2"} plugins string. If empty/absent, the brand
   * defaults to {@code "vanilla"} and the plugin list is empty (not every server exposes plugins).
   */
  private static ParsedPlugins parsePlugins(String plugins) {
    if (plugins == null || plugins.isEmpty()) {
      return new ParsedPlugins("vanilla", List.of());
    }

    String[] parts = plugins.split(":", 2);
    String brand = parts[0].strip();
    List<String> parsedPlugins =
        parts.length == 2
            ? Arrays.stream(parts[1].split(";")).map(String::strip).toList()
            : List.of();

    return new ParsedPlugins(brand, parsedPlugins);
  }
}
