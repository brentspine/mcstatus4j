package de.brentspine.mcstatus4j.responses;

/** Player-count information from a Legacy (pre-1.7) Java edition status response. */
public record LegacyStatusPlayers(int online, int max) implements BaseStatusPlayers {}
