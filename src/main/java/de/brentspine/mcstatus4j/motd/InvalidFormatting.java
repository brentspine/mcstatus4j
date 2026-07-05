package de.brentspine.mcstatus4j.motd;

/**
 * An unrecognized formatting/color code. Might be genuinely invalid, or a newer code mcstatus4j
 * doesn't yet recognize.
 */
public record InvalidFormatting(String value) implements MotdComponent {}
