package de.brentspine.mcstatus4j.motd;

import java.util.Optional;

/** Java edition {@code §}-code formatting. See the Minecraft wiki's Formatting codes page. */
public enum JavaFormatting implements AnyFormatting {
  BOLD("l"),
  ITALIC("o"),
  UNDERLINED("n"),
  STRIKETHROUGH("m"),
  OBFUSCATED("k"),
  RESET("r");

  private final String code;

  JavaFormatting(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Look up a formatting code by its {@link #code()} value, case-insensitively. */
  public static Optional<JavaFormatting> fromCode(String code) {
    for (JavaFormatting formatting : values()) {
      if (formatting.code.equalsIgnoreCase(code)) {
        return Optional.of(formatting);
      }
    }
    return Optional.empty();
  }
}
