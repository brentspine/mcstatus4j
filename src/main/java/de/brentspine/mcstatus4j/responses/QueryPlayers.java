package de.brentspine.mcstatus4j.responses;

import java.util.List;
import java.util.Map;

/** Player-count and player-list information from a GameSpy4 Query response. */
public record QueryPlayers(int online, int max, List<String> list) {

  static QueryPlayers build(Map<String, Object> raw, List<String> playersList) {
    return new QueryPlayers(
        Integer.parseInt(RawJson.requireString(raw, "numplayers")),
        Integer.parseInt(RawJson.requireString(raw, "maxplayers")),
        playersList);
  }
}
