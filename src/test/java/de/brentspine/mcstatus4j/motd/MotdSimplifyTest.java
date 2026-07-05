package de.brentspine.mcstatus4j.motd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/motd/test_simplifies.py}. */
class MotdSimplifyTest {

  @Test
  void simplifyReturnsNewInstanceWithoutMutatingOriginal() {
    List<MotdComponent> parsed = List.of(JavaFormatting.RESET);
    Motd motd = new Motd(parsed, "", false);
    assertEquals(List.of(), motd.simplify().parsed());
    assertEquals(parsed, motd.parsed());
  }

  @Test
  void simplifyRunsSeveralTimes() {
    Motd motd =
        new Motd(
            List.of(JavaFormatting.BOLD, JavaFormatting.RESET, JavaMinecraftColor.RED), "", false);
    assertEquals(List.of(), motd.simplify().parsed());
  }

  @Test
  void getDoubleColors() {
    assertEquals(
        Set.of(0),
        MotdSimplifier.getDoubleColors(List.of(JavaMinecraftColor.RED, JavaMinecraftColor.BLUE)));
    assertEquals(
        Set.of(0),
        MotdSimplifier.getDoubleColors(
            List.of(WebColor.fromHex("#ff0000"), JavaMinecraftColor.BLUE)));
  }

  @Test
  void getDoubleColorsWithThreeItems() {
    assertEquals(
        Set.of(0, 1),
        MotdSimplifier.getDoubleColors(
            List.of(JavaMinecraftColor.RED, JavaMinecraftColor.BLUE, JavaMinecraftColor.BLUE)));
  }

  @Test
  void getDoubleColorsWithNoDoubleColors() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getDoubleColors(
            List.of(JavaMinecraftColor.RED, new TextComponent("foo"), JavaMinecraftColor.BLUE)));
  }

  @Test
  void getDoubleItems() {
    assertEquals(
        Set.of(0),
        MotdSimplifier.getDoubleItems(List.of(JavaFormatting.BOLD, JavaFormatting.BOLD)));
    assertEquals(
        Set.of(0),
        MotdSimplifier.getDoubleItems(List.of(JavaMinecraftColor.RED, JavaMinecraftColor.RED)));
  }

  @Test
  void getDoubleItemsWithThreeItems() {
    assertEquals(
        Set.of(0, 1),
        MotdSimplifier.getDoubleItems(
            List.of(JavaFormatting.BOLD, JavaFormatting.BOLD, JavaFormatting.BOLD)));
  }

  @Test
  void getDoubleItemsWithNoDoubleItems() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getDoubleItems(
            List.of(JavaFormatting.BOLD, new TextComponent("foo"), JavaFormatting.BOLD)));
  }

  @Test
  void getFormattingBeforeColor() {
    assertEquals(
        Set.of(0),
        MotdSimplifier.getFormattingBeforeColor(
            List.of(JavaFormatting.BOLD, JavaMinecraftColor.RED)));
  }

  @Test
  void getFormattingBeforeColorWithoutFormattingBeforeColor() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getFormattingBeforeColor(
            List.of(JavaFormatting.RESET, new TextComponent("abc"), JavaMinecraftColor.WHITE)));
  }

  @Test
  void skipGetFormattingBeforeColor() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getFormattingBeforeColor(
            List.of(
                new TextComponent("abc"),
                JavaFormatting.BOLD,
                new TextComponent("def"),
                JavaFormatting.RESET,
                new TextComponent("ghi"))));
  }

  @Test
  void getFormattingBeforeColorIfSpaceBetween() {
    assertEquals(
        Set.of(0),
        MotdSimplifier.getFormattingBeforeColor(
            List.of(JavaFormatting.BOLD, new TextComponent(" "), JavaMinecraftColor.RED)));
  }

  @Test
  void twoFormattingsBeforeMinecraftColor() {
    assertEquals(
        Set.of(0, 1),
        MotdSimplifier.getFormattingBeforeColor(
            List.of(JavaFormatting.BOLD, JavaFormatting.ITALIC, JavaMinecraftColor.RED)));
  }

  @Test
  void twoFormattingsOneByOneSimplifyToNothing() {
    Motd motd = new Motd(List.of(JavaFormatting.BOLD, JavaFormatting.ITALIC), "", false);
    assertEquals(List.of(), motd.simplify().parsed());
  }

  @Test
  void nonTextInTheEnd() {
    assertEquals(
        Set.of(5),
        MotdSimplifier.getEndNonText(
            List.of(
                new TextComponent("abc"),
                JavaFormatting.BOLD,
                new TextComponent("def"),
                JavaFormatting.RESET,
                new TextComponent("ghi"),
                JavaMinecraftColor.RED)));
  }

  @Test
  void translationTagInTheEndIsNotRemoved() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getEndNonText(
            List.of(
                new TextComponent("abc"),
                JavaFormatting.BOLD,
                new TextComponent("def"),
                JavaFormatting.RESET,
                new TextComponent("ghi"),
                new TranslationTag("key"))));
  }

  @Test
  void meaninglessResetsAndColorsActive() {
    assertEquals(
        Set.of(2),
        MotdSimplifier.getMeaninglessResetsAndColors(
            List.of(
                JavaFormatting.BOLD,
                new TextComponent("foo"),
                JavaFormatting.BOLD,
                new TextComponent("bar"))));
  }

  @Test
  void meaninglessResetsAndColorsResetNothing() {
    assertEquals(
        Set.of(1),
        MotdSimplifier.getMeaninglessResetsAndColors(
            List.of(new TextComponent("foo"), JavaFormatting.RESET, new TextComponent("bar"))));
  }

  @Test
  void meaninglessResetsAndColorsResets() {
    assertEquals(
        Set.of(),
        MotdSimplifier.getMeaninglessResetsAndColors(
            List.of(
                JavaFormatting.BOLD,
                new TextComponent("foo"),
                JavaFormatting.RESET,
                JavaFormatting.BOLD,
                new TextComponent("bar"))));
  }

  @Test
  void simplifyPreservesRawReference() {
    Object raw = new Object();
    Motd motd = new Motd(List.of(), raw, false);
    assertEquals(raw, motd.simplify().raw());
  }

  @Test
  void simplifyDoesNotRemoveStringContainingOnlySpaces() {
    Motd motd = new Motd(List.of(new TextComponent(" ".repeat(20))), "", false);
    assertEquals(List.of(new TextComponent(" ".repeat(20))), motd.simplify().parsed());
  }

  @Test
  void simplifyMeaninglessResetsAndColors() {
    Motd motd = Motd.parse("&a1&a2&a3", false);
    assertEquals(
        List.of(JavaMinecraftColor.GREEN, new TextComponent("123")), motd.simplify().parsed());
  }

  @Test
  void removeFormattingResetIfThereWasNoColorOrFormatting() {
    java.util.Map<String, Object> input =
        java.util.Map.of("text", "123", "extra", List.of(java.util.Map.of("text", "123")));
    Motd motd = Motd.parse(input, false);
    assertEquals(
        List.of(
            new TextComponent("123"),
            JavaFormatting.RESET,
            new TextComponent("123"),
            JavaFormatting.RESET),
        motd.parsed());
    assertEquals(List.of(new TextComponent("123123")), motd.simplify().parsed());
  }

  @Test
  void squashNearbyStrings() {
    Motd motd =
        new Motd(
            List.of(new TextComponent("123"), new TextComponent("123"), new TextComponent("123")),
            "",
            false);
    assertEquals(List.of(new TextComponent("123123123")), motd.simplify().parsed());
  }
}
