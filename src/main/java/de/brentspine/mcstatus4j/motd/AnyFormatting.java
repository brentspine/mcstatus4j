package de.brentspine.mcstatus4j.motd;

/**
 * Common supertype for {@link JavaFormatting} and {@link BedrockFormatting}, so parsing/rendering
 * code can handle either edition's formatting codes uniformly (mirrors Python's {@code
 * AnyFormatting} type alias, which relies on {@code isinstance} checks against a union).
 */
public sealed interface AnyFormatting extends MotdComponent
    permits JavaFormatting, BedrockFormatting {

  /**
   * The single-character formatting code (e.g. {@code "l"} for bold), as used after a {@code §}.
   */
  String code();

  /**
   * The enum constant name (e.g. {@code "RESET"}). Declaring this here (satisfied automatically by
   * {@code Enum#name()} on any implementing enum) lets simplifier/transformer code branch on it
   * through the interface type without knowing which concrete edition enum it's dealing with.
   */
  String name();
}
