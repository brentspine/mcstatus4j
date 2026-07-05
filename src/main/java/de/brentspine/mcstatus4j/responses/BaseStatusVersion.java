package de.brentspine.mcstatus4j.responses;

/** Shared shape of version information across editions. */
public sealed interface BaseStatusVersion
    permits JavaStatusVersion, BedrockStatusVersion, LegacyStatusVersion {

  /** The version name, e.g. {@code "1.19.3"}. */
  String name();

  /** The protocol version number, e.g. {@code 761}. */
  int protocol();
}
