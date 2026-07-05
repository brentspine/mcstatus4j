package de.brentspine.mcstatus4j.motd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/motd/test_motd_parse.py}. */
class MotdParseTest {

  @Test
  void correctResultBedrock() {
    Motd motd = Motd.parse(MotdFixtures.SOURCE_BEDROCK, true);
    List<MotdComponent> expected =
        List.of(
            new TextComponent("1"),
            BedrockMinecraftColor.BLACK,
            BedrockFormatting.OBFUSCATED,
            new TextComponent("2"),
            BedrockMinecraftColor.DARK_BLUE,
            BedrockFormatting.BOLD,
            new TextComponent("3"),
            BedrockMinecraftColor.DARK_GREEN,
            BedrockFormatting.ITALIC,
            new TextComponent("4"),
            BedrockMinecraftColor.DARK_AQUA,
            new TextComponent("5"),
            BedrockMinecraftColor.DARK_RED,
            new TextComponent("6"),
            BedrockMinecraftColor.DARK_PURPLE,
            new TextComponent("7"),
            BedrockMinecraftColor.GOLD,
            new TextComponent("8"),
            BedrockMinecraftColor.GRAY,
            new TextComponent("9"),
            BedrockMinecraftColor.DARK_GRAY,
            new TextComponent("10"),
            BedrockMinecraftColor.BLUE,
            new TextComponent("11"),
            BedrockMinecraftColor.GREEN,
            new TextComponent("12"),
            BedrockMinecraftColor.AQUA,
            new TextComponent("13"),
            BedrockMinecraftColor.RED,
            new TextComponent("14"),
            BedrockMinecraftColor.LIGHT_PURPLE,
            new TextComponent("15"),
            BedrockMinecraftColor.YELLOW,
            new TextComponent("16"),
            BedrockMinecraftColor.WHITE,
            new TextComponent("17"),
            BedrockMinecraftColor.MINECOIN_GOLD,
            new TextComponent("18"),
            BedrockMinecraftColor.MATERIAL_QUARTZ,
            new TextComponent("19"),
            BedrockMinecraftColor.MATERIAL_IRON,
            new TextComponent("20"),
            BedrockMinecraftColor.MATERIAL_NETHERITE,
            new TextComponent("21"),
            BedrockMinecraftColor.MATERIAL_REDSTONE,
            new TextComponent("22"),
            BedrockMinecraftColor.MATERIAL_COPPER,
            new TextComponent("23"),
            BedrockMinecraftColor.MATERIAL_GOLD,
            new TextComponent("24"),
            BedrockMinecraftColor.MATERIAL_EMERALD,
            new TextComponent("25"),
            BedrockMinecraftColor.MATERIAL_DIAMOND,
            new TextComponent("26"),
            BedrockMinecraftColor.MATERIAL_LAPIS,
            new TextComponent("27"),
            BedrockMinecraftColor.MATERIAL_AMETHYST,
            new TextComponent("28"),
            BedrockMinecraftColor.MATERIAL_RESIN,
            new TextComponent("29"),
            new InvalidFormatting("z"),
            new TextComponent("30"),
            BedrockFormatting.RESET,
            new TextComponent("31"));
    assertEquals(expected, motd.parsed());
    assertEquals(MotdFixtures.SOURCE_BEDROCK, motd.raw());
  }

  @Test
  void parseAsStrIgnoreMinecoinGoldOnJava() {
    assertEquals(List.of(BedrockMinecraftColor.MINECOIN_GOLD), Motd.parse("&g", true).parsed());
    assertEquals(List.of(new InvalidFormatting("g")), Motd.parse("&g", false).parsed());
  }

  @Test
  void parseAsStrIgnoreMaterialColorsOnJava() {
    assertEquals(List.of(BedrockMinecraftColor.MATERIAL_IRON), Motd.parse("&i", true).parsed());
    assertEquals(List.of(new InvalidFormatting("i")), Motd.parse("&i", false).parsed());
  }

  @Test
  void parseAsStrUnderlinedOnJava() {
    assertEquals(List.of(BedrockMinecraftColor.MATERIAL_COPPER), Motd.parse("&n", true).parsed());
    assertEquals(List.of(JavaFormatting.UNDERLINED), Motd.parse("&n", false).parsed());
  }

  @Test
  void parseIncorrectFormatting() {
    assertEquals(List.of(new InvalidFormatting("z")), Motd.parse("&z", false).parsed());
  }

  @Test
  void parseUppercasePasses() {
    assertEquals(List.of(JavaMinecraftColor.GREEN), Motd.parse("&A", false).parsed());
  }

  @Test
  void emptyStringInputIsEmpty() {
    assertEquals(List.of(), Motd.parse("", false).parsed());
  }

  @Test
  void emptyListInputYieldsReset() {
    assertEquals(List.of(JavaFormatting.RESET), Motd.parse(List.of(), false).parsed());
  }

  @Test
  void emptyExtraAndTextYieldsReset() {
    Map<String, Object> input = Map.of("extra", List.of(), "text", "");
    assertEquals(List.of(JavaFormatting.RESET), Motd.parse(input, false).parsed());
  }

  @Test
  void topLevelFormattingAppliesToAllInExtra() {
    Map<String, Object> input =
        Map.of(
            "text",
            "top",
            "bold",
            true,
            "extra",
            List.of(Map.of("color", "red", "text", "not top")));
    assertEquals(
        List.of(
            JavaFormatting.BOLD,
            new TextComponent("top"),
            JavaFormatting.RESET,
            JavaFormatting.BOLD,
            JavaMinecraftColor.RED,
            new TextComponent("not top"),
            JavaFormatting.RESET),
        Motd.parse(input, false).parsed());
  }

  @Test
  void topLevelFormattingCanBeOverwritten() {
    Map<String, Object> input =
        Map.of(
            "text",
            "bold",
            "bold",
            true,
            "extra",
            List.of(Map.of("color", "red", "bold", false, "text", "not bold")));
    assertEquals(
        List.of(
            JavaFormatting.BOLD,
            new TextComponent("bold"),
            JavaFormatting.RESET,
            JavaMinecraftColor.RED,
            new TextComponent("not bold"),
            JavaFormatting.RESET),
        Motd.parse(input, false).parsed());
  }

  @Test
  void topLevelFormattingAppliesToStringInsideExtra() {
    Map<String, Object> input = Map.of("text", "top", "bold", true, "extra", List.of("not top"));
    assertEquals(
        List.of(
            JavaFormatting.BOLD,
            new TextComponent("top"),
            JavaFormatting.RESET,
            JavaFormatting.BOLD,
            new TextComponent("not top")),
        Motd.parse(input, false).parsed());
  }

  @Test
  void formattingKeySetToFalseWithoutBeingSetTrueBefore() {
    Map<String, Object> input = Map.of("color", "red", "bold", false, "text", "not bold");
    assertEquals(
        List.of(JavaMinecraftColor.RED, new TextComponent("not bold"), JavaFormatting.RESET),
        Motd.parse(input, false).parsed());
  }

  @Test
  void translateString() {
    Map<String, Object> input = Map.of("translate", "the key");
    assertEquals(
        List.of(new TranslationTag("the key"), JavaFormatting.RESET),
        Motd.parse(input, false).parsed());
  }

  @Test
  void shortTextIsNotConsideredAsColor() {
    assertEquals(List.of(new TextComponent("a")), Motd.parse("a", false).parsed());
  }

  @Test
  void textFieldContainsFormatting() {
    Map<String, Object> input = Map.of("text", "&aHello!");
    assertEquals(
        List.of(JavaMinecraftColor.GREEN, new TextComponent("Hello!"), JavaFormatting.RESET),
        Motd.parse(input, false).parsed());
  }

  @Test
  void invalidRawInputThrows() {
    Object obj = new Object();
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Motd.parse(obj, false));
    assertEquals(
        "Expected list, string or map data, got " + obj.getClass() + " (" + obj + "), report this!",
        ex.getMessage());
  }

  @Test
  void rawAttributePreservesOriginalValue() {
    Motd motd = Motd.parse(MotdFixtures.SOURCE_BEDROCK, true);
    assertEquals(MotdFixtures.SOURCE_BEDROCK, motd.raw());
  }
}
