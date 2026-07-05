package de.brentspine.mcstatus4j.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/responses/test_java.py}. */
class JavaStatusResponseTest {

  private static Map<String, Object> rawFixture() {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("players", Map.of("max", 20, "online", 0));
    raw.put("version", Map.of("name", "1.8-pre1", "protocol", 44));
    raw.put("description", "A Minecraft Server");
    raw.put("enforcesSecureChat", true);
    raw.put("favicon", "data:image/png;base64,foo");
    return raw;
  }

  @Test
  void buildsExpectedFields() {
    JavaStatusResponse response = JavaStatusResponse.build(rawFixture(), 0);

    assertEquals(new JavaStatusPlayers(0, 20, null), response.players());
    assertEquals(new JavaStatusVersion("1.8-pre1", 44), response.version());
    assertEquals("A Minecraft Server", response.motd().toMinecraft());
    assertEquals(0.0, response.latency());
    assertEquals(true, response.enforcesSecureChat());
    assertEquals("data:image/png;base64,foo", response.icon());
    assertEquals(rawFixture(), response.raw());
  }

  @Test
  void optionalFieldsBecomeNullWhenAbsent() {
    Map<String, Object> raw = rawFixture();
    raw.remove("favicon");
    raw.remove("enforcesSecureChat");
    JavaStatusResponse response = JavaStatusResponse.build(raw, 0);

    assertNull(response.icon());
    assertNull(response.enforcesSecureChat());
  }

  @Test
  void asDict() {
    JavaStatusResponse response = JavaStatusResponse.build(rawFixture(), 0);
    Map<String, Object> dict = response.asDict();

    assertEquals(true, dict.get("enforcesSecureChat"));
    assertEquals("data:image/png;base64,foo", dict.get("icon"));
    assertEquals(0.0, dict.get("latency"));
    assertEquals("A Minecraft Server", dict.get("motd"));
    assertEquals(Map.of("online", 0, "max", 20), dict.get("players"));
    assertEquals(Map.of("name", "1.8-pre1", "protocol", 44), dict.get("version"));
    assertEquals(rawFixture(), dict.get("raw"));
  }

  @Test
  void descriptionAliasesMotdToMinecraft() {
    JavaStatusResponse response = JavaStatusResponse.build(rawFixture(), 0);
    assertEquals("A Minecraft Server", response.description());
  }

  @Test
  void playersSampleParsed() {
    Map<String, Object> raw =
        Map.of(
            "max",
            20,
            "online",
            0,
            "sample",
            List.of(
                Map.of("name", "foo", "id", "0b3717c4-f45d-47c8-b8e2-3d9ff6f93a89"),
                Map.of("name", "bar", "id", "61699b2e-d327-4a01-9f1e-0ea8c3f06bc6")));
    JavaStatusPlayers players = JavaStatusPlayers.build(raw);

    assertEquals(20, players.max());
    assertEquals(0, players.online());
    assertEquals(
        List.of(
            new JavaStatusPlayer("foo", "0b3717c4-f45d-47c8-b8e2-3d9ff6f93a89"),
            new JavaStatusPlayer("bar", "61699b2e-d327-4a01-9f1e-0ea8c3f06bc6")),
        players.sample());
  }

  @Test
  void emptySampleBecomesEmptyList() {
    Map<String, Object> raw = Map.of("max", 20, "online", 0, "sample", List.of());
    assertEquals(List.of(), JavaStatusPlayers.build(raw).sample());
  }

  @Test
  void nullSampleStaysNull() {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("max", 123);
    raw.put("online", 1);
    raw.put("sample", null);
    assertNull(JavaStatusPlayers.build(raw).sample());
  }

  @Test
  void playerUuidAliasesId() {
    JavaStatusPlayer player =
        JavaStatusPlayer.build(Map.of("name", "foo", "id", "0b3717c4-f45d-47c8-b8e2-3d9ff6f93a89"));
    assertEquals(player.id(), player.uuid());
    assertTrue(player.id() == player.uuid());
  }

  @Test
  void versionBuild() {
    JavaStatusVersion version = JavaStatusVersion.build(Map.of("name", "1.8-pre1", "protocol", 44));
    assertEquals("1.8-pre1", version.name());
    assertEquals(44, version.protocol());
  }
}
