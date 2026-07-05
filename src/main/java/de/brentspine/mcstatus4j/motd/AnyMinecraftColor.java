package de.brentspine.mcstatus4j.motd;

/**
 * Common supertype for {@link JavaMinecraftColor} and {@link BedrockMinecraftColor} (mirrors
 * Python's {@code AnyMinecraftColor} type alias).
 */
public sealed interface AnyMinecraftColor extends MotdComponent
    permits JavaMinecraftColor, BedrockMinecraftColor {

  /** The single-character color code (e.g. {@code "c"} for red), as used after a {@code §}. */
  String code();

  /**
   * The enum constant name (e.g. {@code "GRAY"}). Declaring this here (satisfied automatically by
   * {@code Enum#name()} on any implementing enum) lets simplifier/transformer code look up an
   * edition-specific color table entry through the interface type.
   */
  String name();
}
