package de.brentspine.mcstatus4j.responses;

import de.brentspine.mcstatus4j.motd.Motd;
import java.util.List;
import java.util.Map;

/**
 * The response for {@code BedrockServer.status()}.
 *
 * <p>Built from the semicolon-delimited field list decoded off a RakNet Unconnected Pong packet:
 * {@code [0]}=brand, {@code [1]}=motd, {@code [2]}=protocol, {@code [3]}=version name, {@code
 * [4]}=online players, {@code [5]}=max players, ({@code [6]}=server GUID, unused), {@code [7]}=map
 * name (optional), {@code [8]}=gamemode (optional).
 */
public record BedrockStatusResponse(
    BedrockStatusPlayers players,
    BedrockStatusVersion version,
    Motd motd,
    double latency,
    String mapName,
    String gamemode)
    implements BaseStatusResponse {

  public static BedrockStatusResponse build(List<Object> decodedData, double latency) {
    String mapName = decodedData.size() > 7 ? String.valueOf(decodedData.get(7)) : null;
    String gamemode = decodedData.size() > 8 ? String.valueOf(decodedData.get(8)) : null;

    return new BedrockStatusResponse(
        new BedrockStatusPlayers(
            Integer.parseInt(String.valueOf(decodedData.get(4))),
            Integer.parseInt(String.valueOf(decodedData.get(5)))),
        new BedrockStatusVersion(
            String.valueOf(decodedData.get(3)),
            Integer.parseInt(String.valueOf(decodedData.get(2))),
            String.valueOf(decodedData.get(0))),
        Motd.parse(decodedData.get(1), true),
        latency,
        mapName,
        gamemode);
  }

  @Override
  public Map<String, Object> asDict() {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("players", Map.of("online", players.online(), "max", players.max()));
    result.put(
        "version",
        Map.of("name", version.name(), "protocol", version.protocol(), "brand", version.brand()));
    result.put("motd", motd.simplify().toMinecraft());
    result.put("latency", latency);
    result.put("mapName", mapName);
    result.put("gamemode", gamemode);
    return result;
  }
}
