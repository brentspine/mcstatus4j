package de.brentspine.mcstatus4j.motd;

/**
 * A plain text chunk of a MOTD. Stands in for Python's bare {@code str} entries in {@code
 * ParsedMotdComponent} - see {@link MotdComponent}'s class docs for why this wrapper exists.
 */
public record TextComponent(String value) implements MotdComponent {}
