package de.brentspine.mcstatus4j.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/responses/test_query.py}. */
class QueryResponseTest {

  private static Map<String, Object> rawFixture() {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("hostname", "A Minecraft Server");
    raw.put("gametype", "GAME TYPE");
    raw.put("game_id", "GAME ID");
    raw.put("version", "1.8");
    raw.put("plugins", "");
    raw.put("map", "world");
    raw.put("numplayers", "3");
    raw.put("maxplayers", "20");
    raw.put("hostport", "9999");
    raw.put("hostip", "192.168.56.1");
    return raw;
  }

  private static final List<String> RAW_PLAYERS = List.of("Dinnerbone", "Djinnibone", "Steve");

  @Test
  void buildsExpectedFields() {
    QueryResponse response = QueryResponse.build(rawFixture(), RAW_PLAYERS);

    assertEquals(rawFixture(), response.raw());
    assertEquals("A Minecraft Server", response.motd().toMinecraft());
    assertEquals("world", response.mapName());
    assertEquals(new QueryPlayers(3, 20, RAW_PLAYERS), response.players());
    assertEquals(new QuerySoftware("1.8", "vanilla", List.of()), response.software());
    assertEquals("192.168.56.1", response.ip());
    assertEquals(9999, response.port());
    assertEquals("GAME TYPE", response.gameType());
    assertEquals("GAME ID", response.gameId());
  }

  @Test
  void asDict() {
    Map<String, Object> dict = QueryResponse.build(rawFixture(), RAW_PLAYERS).asDict();

    assertEquals("GAME ID", dict.get("gameId"));
    assertEquals("GAME TYPE", dict.get("gameType"));
    assertEquals("192.168.56.1", dict.get("ip"));
    assertEquals("world", dict.get("mapName"));
    assertEquals("A Minecraft Server", dict.get("motd"));
    assertEquals(Map.of("online", 3, "max", 20, "list", RAW_PLAYERS), dict.get("players"));
    assertEquals(9999, dict.get("port"));
    assertEquals(rawFixture(), dict.get("raw"));
    assertEquals(
        Map.of("brand", "vanilla", "plugins", List.of(), "version", "1.8"), dict.get("software"));
  }

  @Test
  void queryPlayersBuild() {
    Map<String, Object> raw = new LinkedHashMap<>(rawFixture());
    raw.put("hostport", "25565");
    QueryPlayers players = QueryPlayers.build(raw, RAW_PLAYERS);
    assertEquals(3, players.online());
    assertEquals(20, players.max());
    assertEquals(RAW_PLAYERS, players.list());
  }

  @Test
  void softwareVanilla() {
    QuerySoftware software = QuerySoftware.build("1.8", "");
    assertEquals("vanilla", software.brand());
    assertEquals("1.8", software.version());
    assertEquals(List.of(), software.plugins());
  }

  @Test
  void softwareModded() {
    QuerySoftware software =
        QuerySoftware.build("1.8", "A modded server: Foo 1.0; Bar 2.0; Baz 3.0");
    assertEquals("A modded server", software.brand());
    assertEquals(List.of("Foo 1.0", "Bar 2.0", "Baz 3.0"), software.plugins());
  }

  @Test
  void softwareModdedNoPlugins() {
    QuerySoftware software = QuerySoftware.build("1.8", "A modded server");
    assertEquals("A modded server", software.brand());
    assertEquals(List.of(), software.plugins());
  }
}
