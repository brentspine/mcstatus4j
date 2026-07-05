package de.brentspine.mcstatus4j.motd;

import java.util.Optional;

/** Java edition {@code §}-code colors. See the Minecraft wiki's Formatting codes page. */
public enum JavaMinecraftColor implements AnyMinecraftColor {
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
  WHITE("f");

  private final String code;

  JavaMinecraftColor(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Look up a color code by its {@link #code()} value, case-insensitively. */
  public static Optional<JavaMinecraftColor> fromCode(String code) {
    for (JavaMinecraftColor color : values()) {
      if (color.code.equalsIgnoreCase(code)) {
        return Optional.of(color);
      }
    }
    return Optional.empty();
  }
}
