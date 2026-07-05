package de.brentspine.mcstatus4j.responses;

/** Player-count information from a Bedrock edition status response. */
public record BedrockStatusPlayers(int online, int max) implements BaseStatusPlayers {}
