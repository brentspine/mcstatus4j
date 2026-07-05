package de.brentspine.mcstatus4j.responses;

import de.brentspine.mcstatus4j.motd.Motd;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The response for {@code JavaServer.status()}.
 *
 * <p>Forge/modded-server metadata ({@code forgeData}/{@code modinfo} in the raw response, Python's
 * {@code ForgeData}) is deliberately not represented here yet - it's the single most intricate part
 * of the whole protocol (a custom bit-packed binary format) and is deferred to a later milestone
 * since it has no bearing on basic status/ping/query functionality. See the project's CLAUDE.md.
 */
public record JavaStatusResponse(
    Map<String, Object> raw,
    JavaStatusPlayers players,
    JavaStatusVersion version,
    Motd motd,
    double latency,
    Boolean enforcesSecureChat,
    String icon)
    implements BaseStatusResponse {

  /**
   * Build a {@link JavaStatusResponse} from the raw JSON tree returned by the server (as parsed by
   * e.g. Jackson into nested {@code Map}/{@code List}/{@code String}/{@code Number}/{@code
   * Boolean}).
   *
   * @throws java.util.NoSuchElementException if a required field ({@code players}, {@code version})
   *     is missing.
   * @throws ClassCastException if a required field has the wrong type.
   */
  public static JavaStatusResponse build(Map<String, Object> raw, double latency) {
    return new JavaStatusResponse(
        raw,
        JavaStatusPlayers.build(RawJson.asMap(RawJson.require(raw, "players"))),
        JavaStatusVersion.build(RawJson.asMap(RawJson.require(raw, "version"))),
        Motd.parse(raw.getOrDefault("description", ""), false),
        latency,
        (Boolean) raw.get("enforcesSecureChat"),
        (String) raw.get("favicon"));
  }

  @Override
  public Map<String, Object> asDict() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("raw", raw);
    result.put("players", Map.of("online", players.online(), "max", players.max()));
    result.put("version", Map.of("name", version.name(), "protocol", version.protocol()));
    result.put("motd", motd.simplify().toMinecraft());
    result.put("latency", latency);
    result.put("enforcesSecureChat", enforcesSecureChat);
    result.put("icon", icon);
    return result;
  }
}
