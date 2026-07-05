package de.brentspine.mcstatus4j.responses;

/** Shared shape of player-count information across editions. */
public sealed interface BaseStatusPlayers
    permits JavaStatusPlayers, BedrockStatusPlayers, LegacyStatusPlayers {

  /** Current number of online players. */
  int online();

  /** The maximum allowed number of players (aka server slots). */
  int max();
}
