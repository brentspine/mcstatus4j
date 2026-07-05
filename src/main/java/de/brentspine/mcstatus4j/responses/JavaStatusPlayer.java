package de.brentspine.mcstatus4j.responses;

import java.util.Map;

/** A single player entry from the Java edition status response's player sample list. */
public record JavaStatusPlayer(String name, String id) {

  /** Alias for {@link #id()}. */
  public String uuid() {
    return id;
  }

  static JavaStatusPlayer build(Map<String, Object> raw) {
    return new JavaStatusPlayer(
        RawJson.requireString(raw, "name"), RawJson.requireString(raw, "id"));
  }
}
