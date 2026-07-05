package de.brentspine.mcstatus4j.responses;

import de.brentspine.mcstatus4j.motd.Motd;
import java.util.List;
import java.util.Map;

/**
 * The response for {@code LegacyServer.status()} (pre-1.7 Java edition Server List Ping).
 *
 * <p>Built from a 5-element decoded kick-packet list: {@code [0]}=protocol, {@code [1]}=version
 * name, {@code [2]}=motd, {@code [3]}=online players, {@code [4]}=max players.
 */
public record LegacyStatusResponse(
    LegacyStatusPlayers players, LegacyStatusVersion version, Motd motd, double latency)
    implements BaseStatusResponse {

  public static LegacyStatusResponse build(List<String> decodedData, double latency) {
    return new LegacyStatusResponse(
        new LegacyStatusPlayers(
            Integer.parseInt(decodedData.get(3)), Integer.parseInt(decodedData.get(4))),
        new LegacyStatusVersion(decodedData.get(1), Integer.parseInt(decodedData.get(0))),
        Motd.parse(decodedData.get(2), false),
        latency);
  }

  @Override
  public Map<String, Object> asDict() {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("players", Map.of("online", players.online(), "max", players.max()));
    result.put("version", Map.of("name", version.name(), "protocol", version.protocol()));
    result.put("motd", motd.simplify().toMinecraft());
    result.put("latency", latency);
    return result;
  }
}
