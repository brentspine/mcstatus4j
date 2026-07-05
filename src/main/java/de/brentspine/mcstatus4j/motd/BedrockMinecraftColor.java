package de.brentspine.mcstatus4j.motd;

import java.util.Optional;

/** Bedrock edition {@code §}-code colors: the 16 standard colors plus extended material colors. */
public enum BedrockMinecraftColor implements AnyMinecraftColor {
  BLACK("0"),
  DARK_BLUE("1"),
  DARK_GREEN("2"),
  DARK_AQUA("3"),
  DARK_RED("4"),
  DARK_PURPLE("5"),
  GOLD("6"),
  GRAY("7"),
  DARK_GRAY("8"),
  BLUE("9"),
  GREEN("a"),
  AQUA("b"),
  RED("c"),
  LIGHT_PURPLE("d"),
  YELLOW("e"),
  WHITE("f"),
  MINECOIN_GOLD("g"),
  MATERIAL_QUARTZ("h"),
  MATERIAL_IRON("i"),
  MATERIAL_NETHERITE("j"),
  MATERIAL_REDSTONE("m"),
  MATERIAL_COPPER("n"),
  MATERIAL_GOLD("p"),
  MATERIAL_EMERALD("q"),
  MATERIAL_DIAMOND("s"),
  MATERIAL_LAPIS("t"),
  MATERIAL_AMETHYST("u"),
  MATERIAL_RESIN("v");

  private final String code;

  BedrockMinecraftColor(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Look up a color code by its {@link #code()} value, case-insensitively. */
  public static Optional<BedrockMinecraftColor> fromCode(String code) {
    for (BedrockMinecraftColor color : values()) {
      if (color.code.equalsIgnoreCase(code)) {
        return Optional.of(color);
      }
    }
    return Optional.empty();
  }
}
