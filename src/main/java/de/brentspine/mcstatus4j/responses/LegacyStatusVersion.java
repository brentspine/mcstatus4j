package de.brentspine.mcstatus4j.responses;

/**
 * Version information from a Legacy (pre-1.7) status response.
 *
 * @param name the version name, e.g. {@code "1.6.4"}; {@code "<1.4"} for servers older than that,
 *     since they didn't send real version info.
 * @param protocol the protocol version; {@code -1} for servers older than 1.4, which didn't send
 *     this information at all.
 */
public record LegacyStatusVersion(String name, int protocol) implements BaseStatusVersion {}
