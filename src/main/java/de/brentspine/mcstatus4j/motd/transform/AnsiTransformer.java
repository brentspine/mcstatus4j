package de.brentspine.mcstatus4j.motd.transform;

import de.brentspine.mcstatus4j.motd.AnyFormatting;
import de.brentspine.mcstatus4j.motd.AnyMinecraftColor;
import de.brentspine.mcstatus4j.motd.WebColor;
import java.util.List;
import java.util.Map;

/**
 * Renders a MOTD as 24-bit ANSI escape codes, for printing colored text in a terminal. Backs {@code
 * Motd.toAnsi()}.
 *
 * <p>"Obfuscated" formatting is rendered as blinking text.
 */
public final class AnsiTransformer extends PlainTransformer {

  private static final Map<String, String> FORMATTING_TO_ANSI_TAGS =
      Map.of(
          "BOLD", "1",
          "STRIKETHROUGH", "9",
          "ITALIC", "3",
          "UNDERLINED", "4",
          "OBFUSCATED", "5");

  public AnsiTransformer(boolean bedrock) {
    super(bedrock);
  }

  /** Convert an RGB color to its 24-bit ANSI foreground escape code. */
  public String ansiColor(int r, int g, int b) {
    return "\033[38;2;" + r + ";" + g + ";" + b + "m";
  }

  /** Convert a named Minecraft color to its 24-bit ANSI foreground escape code. */
  public String ansiColor(AnyMinecraftColor color) {
    Map<String, Rgb> foreground =
        bedrock ? MotdColorTables.BEDROCK_FOREGROUND : MotdColorTables.JAVA_FOREGROUND;
    Rgb rgb = foreground.get(color.name());
    return ansiColor(rgb.r(), rgb.g(), rgb.b());
  }

  @Override
  protected String formatOutput(List<String> results) {
    return "\033[0m" + super.formatOutput(results) + "\033[0m";
  }

  @Override
  protected String handleMinecraftColor(AnyMinecraftColor element) {
    return ansiColor(element);
  }

  @Override
  protected String handleWebColor(WebColor element) {
    return ansiColor(element.r(), element.g(), element.b());
  }

  @Override
  protected String handleFormatting(AnyFormatting element) {
    if ("RESET".equals(element.name())) {
      return "\033[0m";
    }
    return "\033[" + FORMATTING_TO_ANSI_TAGS.get(element.name()) + "m";
  }
}
