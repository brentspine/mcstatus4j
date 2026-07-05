package de.brentspine.mcstatus4j.motd;

import java.util.Optional;

/**
 * Bedrock edition {@code §}-code formatting. Same as {@link JavaFormatting} but excludes {@code
 * UNDERLINED}/{@code STRIKETHROUGH}, which don't work on Bedrock (MCPE-41729).
 */
public enum BedrockFormatting implements AnyFormatting {
  BOLD("l"),
  ITALIC("o"),
  OBFUSCATED("k"),
  RESET("r");

  private final String code;

  BedrockFormatting(String code) {
    this.code = code;
  }

  @Override
  public String code() {
    return code;
  }

  /** Look up a formatting code by its {@link #code()} value, case-insensitively. */
  public static Optional<BedrockFormatting> fromCode(String code) {
    for (BedrockFormatting formatting : values()) {
      if (formatting.code.equalsIgnoreCase(code)) {
        return Optional.of(formatting);
      }
    }
    return Optional.empty();
  }
}
