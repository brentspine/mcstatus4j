package de.brentspine.mcstatus4j.motd.transform;

import java.util.Map;

/**
 * Foreground/background (aka "shadow") RGB values for each named Minecraft color, keyed by enum
 * constant name. Ported verbatim from Python mcstatus's {@code motd/_transformers.py} - exact
 * values matter for visual fidelity, including that Java and Bedrock disagree on {@code GRAY}.
 */
final class MotdColorTables {

  private MotdColorTables() {}

  private static final Map<String, Rgb> SHARED_FOREGROUND =
      Map.ofEntries(
          Map.entry("BLACK", new Rgb(0, 0, 0)),
          Map.entry("DARK_BLUE", new Rgb(0, 0, 170)),
          Map.entry("DARK_GREEN", new Rgb(0, 170, 0)),
          Map.entry("DARK_AQUA", new Rgb(0, 170, 170)),
          Map.entry("DARK_RED", new Rgb(170, 0, 0)),
          Map.entry("DARK_PURPLE", new Rgb(170, 0, 170)),
          Map.entry("GOLD", new Rgb(255, 170, 0)),
          Map.entry("GRAY", new Rgb(170, 170, 170)),
          Map.entry("DARK_GRAY", new Rgb(85, 85, 85)),
          Map.entry("BLUE", new Rgb(85, 85, 255)),
          Map.entry("GREEN", new Rgb(85, 255, 85)),
          Map.entry("AQUA", new Rgb(85, 255, 255)),
          Map.entry("RED", new Rgb(255, 85, 85)),
          Map.entry("LIGHT_PURPLE", new Rgb(255, 85, 255)),
          Map.entry("YELLOW", new Rgb(255, 255, 85)),
          Map.entry("WHITE", new Rgb(255, 255, 255)));

  private static final Map<String, Rgb> SHARED_BACKGROUND =
      Map.ofEntries(
          Map.entry("BLACK", new Rgb(0, 0, 0)),
          Map.entry("DARK_BLUE", new Rgb(0, 0, 42)),
          Map.entry("DARK_GREEN", new Rgb(0, 42, 0)),
          Map.entry("DARK_AQUA", new Rgb(0, 42, 42)),
          Map.entry("DARK_RED", new Rgb(42, 0, 0)),
          Map.entry("DARK_PURPLE", new Rgb(42, 0, 42)),
          Map.entry("GOLD", new Rgb(64, 42, 0)),
          Map.entry("GRAY", new Rgb(42, 42, 42)),
          Map.entry("DARK_GRAY", new Rgb(21, 21, 21)),
          Map.entry("BLUE", new Rgb(21, 21, 63)),
          Map.entry("GREEN", new Rgb(21, 63, 21)),
          Map.entry("AQUA", new Rgb(21, 63, 63)),
          Map.entry("RED", new Rgb(63, 21, 21)),
          Map.entry("LIGHT_PURPLE", new Rgb(63, 21, 63)),
          Map.entry("YELLOW", new Rgb(63, 63, 21)),
          Map.entry("WHITE", new Rgb(63, 63, 63)));

  static final Map<String, Rgb> JAVA_FOREGROUND = SHARED_FOREGROUND;
  static final Map<String, Rgb> JAVA_BACKGROUND = SHARED_BACKGROUND;

  static final Map<String, Rgb> BEDROCK_FOREGROUND = mergeBedrockForeground();
  static final Map<String, Rgb> BEDROCK_BACKGROUND = mergeBedrockBackground();

  private static Map<String, Rgb> mergeBedrockForeground() {
    Map<String, Rgb> map = new java.util.HashMap<>(SHARED_FOREGROUND);
    map.put("GRAY", new Rgb(198, 198, 198));
    map.put("MINECOIN_GOLD", new Rgb(221, 214, 5));
    map.put("MATERIAL_QUARTZ", new Rgb(227, 212, 209));
    map.put("MATERIAL_IRON", new Rgb(206, 202, 202));
    map.put("MATERIAL_NETHERITE", new Rgb(68, 58, 59));
    map.put("MATERIAL_REDSTONE", new Rgb(151, 22, 7));
    map.put("MATERIAL_COPPER", new Rgb(180, 104, 77));
    map.put("MATERIAL_GOLD", new Rgb(222, 177, 45));
    map.put("MATERIAL_EMERALD", new Rgb(17, 159, 54));
    map.put("MATERIAL_DIAMOND", new Rgb(44, 186, 168));
    map.put("MATERIAL_LAPIS", new Rgb(33, 73, 123));
    map.put("MATERIAL_AMETHYST", new Rgb(154, 92, 198));
    map.put("MATERIAL_RESIN", new Rgb(235, 114, 20));
    return Map.copyOf(map);
  }

  private static Map<String, Rgb> mergeBedrockBackground() {
    Map<String, Rgb> map = new java.util.HashMap<>(SHARED_BACKGROUND);
    map.put("GRAY", new Rgb(49, 49, 49));
    map.put("MINECOIN_GOLD", new Rgb(55, 53, 1));
    map.put("MATERIAL_QUARTZ", new Rgb(56, 53, 52));
    map.put("MATERIAL_IRON", new Rgb(51, 50, 50));
    map.put("MATERIAL_NETHERITE", new Rgb(17, 14, 14));
    map.put("MATERIAL_REDSTONE", new Rgb(37, 5, 1));
    map.put("MATERIAL_COPPER", new Rgb(45, 26, 19));
    map.put("MATERIAL_GOLD", new Rgb(55, 44, 11));
    map.put("MATERIAL_EMERALD", new Rgb(4, 40, 13));
    map.put("MATERIAL_DIAMOND", new Rgb(11, 46, 42));
    map.put("MATERIAL_LAPIS", new Rgb(8, 18, 30));
    map.put("MATERIAL_AMETHYST", new Rgb(38, 23, 49));
    map.put("MATERIAL_RESIN", new Rgb(59, 29, 5));
    return Map.copyOf(map);
  }
}
