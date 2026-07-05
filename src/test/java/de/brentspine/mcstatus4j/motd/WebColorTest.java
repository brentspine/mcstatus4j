package de.brentspine.mcstatus4j.motd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Ported from Python mcstatus's {@code tests/motd/test_components.py::TestWebColor}. */
class WebColorTest {

  @ParameterizedTest
  @CsvSource({"#bfff00,191,255,0", "#00ff80,0,255,128", "#4000ff,64,0,255"})
  void hexToRgbCorrect(String hex, int r, int g, int b) {
    WebColor color = WebColor.fromHex(hex);
    assertEquals(r, color.r());
    assertEquals(g, color.g());
    assertEquals(b, color.b());
  }

  @ParameterizedTest
  @CsvSource({"#bfff00,191,255,0", "#00ff80,0,255,128", "#4000ff,64,0,255"})
  void rgbToHexCorrect(String hex, int r, int g, int b) {
    assertEquals(hex, WebColor.fromRgb(r, g, b).hex());
  }

  @Test
  void hexInOutputHasNumberSign() {
    assertEquals("#bfff00", WebColor.fromHex("#bfff00").hex());
    assertEquals("#4000ff", WebColor.fromHex("4000ff").hex());
  }

  @Test
  void failOnIncorrectHex() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> WebColor.fromHex("#!!!!!!"));
    assertEquals("Failed to parse given hex color: '#!!!!!!'", ex.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 4, 5, 7, 8, 9, 10})
  void failOnTooLongOrTooShortHex(int length) {
    String color = "a".repeat(length);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> WebColor.fromHex(color));
    assertEquals("Got too long/short hex color: '#" + color + "'", ex.getMessage());
  }

  @Test
  void failOnIncorrectRgb() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> WebColor.fromRgb(-23, 699, 1000));
    assertEquals(
        "RGB color byte out of its 8-bit range (0-255) for red (value=-23)", ex.getMessage());
  }

  @Test
  void threeSymbolsHex() {
    assertEquals("#aa11bb", WebColor.fromHex("a1b").hex());
  }
}
