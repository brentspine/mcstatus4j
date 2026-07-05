package de.brentspine.mcstatus4j.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Player-count information from a Java edition status response. */
public record JavaStatusPlayers(int online, int max, List<JavaStatusPlayer> sample)
    implements BaseStatusPlayers {

  static JavaStatusPlayers build(Map<String, Object> raw) {
    List<JavaStatusPlayer> sample = null;
    Object sampleRaw = raw.get("sample");
    if (sampleRaw != null) {
      List<?> list = (List<?>) sampleRaw;
      List<JavaStatusPlayer> built = new ArrayList<>();
      for (Object entry : list) {
        built.add(JavaStatusPlayer.build(RawJson.asMap(entry)));
      }
      sample = List.copyOf(built);
    }
    return new JavaStatusPlayers(
        RawJson.requireInt(raw, "online"), RawJson.requireInt(raw, "max"), sample);
  }
}
