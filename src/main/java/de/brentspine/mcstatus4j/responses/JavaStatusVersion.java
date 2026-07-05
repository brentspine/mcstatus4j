package de.brentspine.mcstatus4j.responses;

import java.util.Map;

/** Version information from a Java edition status response. */
public record JavaStatusVersion(String name, int protocol) implements BaseStatusVersion {

  static JavaStatusVersion build(Map<String, Object> raw) {
    return new JavaStatusVersion(
        RawJson.requireString(raw, "name"), RawJson.requireInt(raw, "protocol"));
  }
}
