package de.brentspine.mcstatus4j.motd;

/**
 * Represents a {@code translate} field in a chat component. Carried through as an opaque tag -
 * mcstatus4j (like Python mcstatus) never resolves/localizes it.
 */
public record TranslationTag(String id) implements MotdComponent {}
