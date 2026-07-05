package de.brentspine.mcstatus4j.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/responses/test_bedrock.py}. */
class BedrockStatusResponseTest {

  private static List<Object> fullFixture() {
    return new ArrayList<>(
        List.of(
            "MCPE",
            "§r§4G§r§6a§r§ey§r§2B§r§1o§r§9w§r§ds§r§4e§r§6r",
            "422",
            "1.18.100500",
            "1",
            "69",
            "3767071975391053022",
            "map name here",
            "Default",
            "1",
            "19132",
            "-1",
            "3"));
  }

  @Test
  void buildsExpectedFields() {
    BedrockStatusResponse response = BedrockStatusResponse.build(fullFixture(), 123.0);

    assertEquals("§r§4G§r§6a§r§ey§r§2B§r§1o§r§9w§r§ds§r§4e§r§6r", response.motd().toMinecraft());
    assertEquals(123.0, response.latency());
    assertEquals("map name here", response.mapName());
    assertEquals("Default", response.gamemode());
    assertEquals(new BedrockStatusPlayers(1, 69), response.players());
    assertEquals(new BedrockStatusVersion("1.18.100500", 422, "MCPE"), response.version());
  }

  @Test
  void mapNameIsNullWhenAbsent() {
    List<Object> parameters =
        new ArrayList<>(List.of("MCPE", "motd", "422", "1.18.100500", "1", "69", "guid"));
    BedrockStatusResponse response = BedrockStatusResponse.build(parameters, 123.0);
    assertNull(response.mapName());
    assertNull(response.gamemode());
  }

  @Test
  void gamemodeIsNullWhenAbsentButMapPresent() {
    List<Object> parameters =
        new ArrayList<>(
            List.of("MCPE", "motd", "422", "1.18.100500", "1", "69", "guid", "map name here"));
    BedrockStatusResponse response = BedrockStatusResponse.build(parameters, 123.0);
    assertEquals("map name here", response.mapName());
    assertNull(response.gamemode());
  }

  @Test
  void asDict() {
    BedrockStatusResponse response = BedrockStatusResponse.build(fullFixture(), 123.0);
    Map<String, Object> dict = response.asDict();

    assertEquals("Default", dict.get("gamemode"));
    assertEquals(123.0, dict.get("latency"));
    assertEquals("map name here", dict.get("mapName"));
    assertEquals("§4G§6a§ey§2B§1o§9w§ds§4e§6r", dict.get("motd"));
    assertEquals(Map.of("online", 1, "max", 69), dict.get("players"));
    assertEquals(
        Map.of("name", "1.18.100500", "protocol", 422, "brand", "MCPE"), dict.get("version"));
  }

  @Test
  void descriptionAliasesMotdToMinecraft() {
    BedrockStatusResponse response = BedrockStatusResponse.build(fullFixture(), 123.0);
    assertEquals("§r§4G§r§6a§r§ey§r§2B§r§1o§r§9w§r§ds§r§4e§r§6r", response.description());
  }
}
