package de.brentspine.mcstatus4j.motd;

/**
 * An arbitrary hex/RGB gradient color found in a MOTD (Minecraft 1.16+ only).
 *
 * <p>Stores the individual {@code r}/{@code g}/{@code b} channels rather than an {@code int[]},
 * since Java arrays don't have value-based {@code equals()}/{@code hashCode()} - required for this
 * record's use as a plain data value (Python's version is a frozen dataclass with a {@code
 * tuple[int,int,int]}, which is naturally value-comparable).
 */
public record WebColor(String hex, int r, int g, int b) implements MotdComponent {

  /**
   * Parse a web color from a hex string, with or without a leading {@code #}, in either 3-digit
   * shorthand ({@code "abc"} -> {@code "aabbcc"}) or full 6-digit form.
   *
   * @throws IllegalArgumentException if the string isn't a valid 3- or 6-digit hex color.
   */
  public static WebColor fromHex(String hexInput) {
    String hex = hexInput.startsWith("#") ? hexInput.substring(1) : hexInput;

    if (hex.length() != 3 && hex.length() != 6) {
      throw new IllegalArgumentException("Got too long/short hex color: '#" + hex + "'");
    }
    if (hex.length() == 3) {
      hex =
          ""
              + hex.charAt(0)
              + hex.charAt(0)
              + hex.charAt(1)
              + hex.charAt(1)
              + hex.charAt(2)
              + hex.charAt(2);
    }

    int r;
    int g;
    int b;
    try {
      r = Integer.parseInt(hex.substring(0, 2), 16);
      g = Integer.parseInt(hex.substring(2, 4), 16);
      b = Integer.parseInt(hex.substring(4, 6), 16);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Failed to parse given hex color: '#" + hex + "'", e);
    }

    return fromRgb(r, g, b);
  }

  /**
   * Construct a web color from individual 8-bit RGB channels.
   *
   * @throws IllegalArgumentException if any channel is outside {@code [0, 255]}.
   */
  public static WebColor fromRgb(int r, int g, int b) {
    checkChannel("red", r);
    checkChannel("green", g);
    checkChannel("blue", b);
    String hex = String.format("#%02x%02x%02x", r, g, b);
    return new WebColor(hex, r, g, b);
  }

  private static void checkChannel(String name, int value) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException(
          "RGB color byte out of its 8-bit range (0-255) for " + name + " (value=" + value + ")");
    }
  }
}
