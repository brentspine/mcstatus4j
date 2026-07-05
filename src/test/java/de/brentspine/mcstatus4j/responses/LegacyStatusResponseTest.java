package de.brentspine.mcstatus4j.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/responses/test_legacy.py}. */
class LegacyStatusResponseTest {

  private static LegacyStatusResponse build() {
    return LegacyStatusResponse.build(
        List.of("47", "1.4.2", "A Minecraft Server", "0", "20"), 123.0);
  }

  @Test
  void buildsExpectedFields() {
    LegacyStatusResponse response = build();
    assertEquals("A Minecraft Server", response.motd().toMinecraft());
    assertEquals(123.0, response.latency());
    assertEquals(new LegacyStatusPlayers(0, 20), response.players());
    assertEquals(new LegacyStatusVersion("1.4.2", 47), response.version());
  }

  @Test
  void asDict() {
    Map<String, Object> dict = build().asDict();
    assertEquals(123.0, dict.get("latency"));
    assertEquals("A Minecraft Server", dict.get("motd"));
    assertEquals(Map.of("online", 0, "max", 20), dict.get("players"));
    assertEquals(Map.of("name", "1.4.2", "protocol", 47), dict.get("version"));
  }
}
