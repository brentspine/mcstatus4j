package de.brentspine.mcstatus4j.responses;

import de.brentspine.mcstatus4j.motd.Motd;
import java.util.List;
import java.util.Map;

/**
 * The response for {@code JavaServer.query()} (GameSpy4 Query protocol).
 *
 * <p>Deliberately not part of the {@link BaseStatusResponse} hierarchy - it has a different shape
 * (its own {@code raw}/{@code motd}/{@code players}/{@code software} rather than a single {@code
 * players}/{@code version} pair), matching Python mcstatus's own {@code QueryResponse} standing
 * apart from {@code BaseStatusResponse}.
 */
public record QueryResponse(
    Map<String, Object> raw,
    Motd motd,
    String mapName,
    QueryPlayers players,
    QuerySoftware software,
    String ip,
    int port,
    String gameType,
    String gameId) {

  public static QueryResponse build(Map<String, Object> raw, List<String> playersList) {
    return new QueryResponse(
        raw,
        Motd.parse(RawJson.requireString(raw, "hostname"), false),
        RawJson.requireString(raw, "map"),
        QueryPlayers.build(raw, playersList),
        QuerySoftware.build(
            RawJson.requireString(raw, "version"), RawJson.requireString(raw, "plugins")),
        RawJson.requireString(raw, "hostip"),
        Integer.parseInt(RawJson.requireString(raw, "hostport")),
        RawJson.requireString(raw, "gametype"),
        RawJson.requireString(raw, "game_id"));
  }

  /**
   * A JSON-serializable {@link Map} view of this response, with {@code motd} rendered as a
   * simplified Minecraft {@code §}-code string rather than the full parsed component tree.
   */
  public Map<String, Object> asDict() {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("raw", raw);
    result.put("motd", motd.simplify().toMinecraft());
    result.put("mapName", mapName);
    result.put(
        "players",
        Map.of("online", players.online(), "max", players.max(), "list", players.list()));
    result.put(
        "software",
        Map.of(
            "version",
            software.version(),
            "brand",
            software.brand(),
            "plugins",
            software.plugins()));
    result.put("ip", ip);
    result.put("port", port);
    result.put("gameType", gameType);
    result.put("gameId", gameId);
    return result;
  }
}
