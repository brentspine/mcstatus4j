package de.brentspine.mcstatus4j.responses;

/**
 * Version information from a Bedrock edition status response.
 *
 * @param brand {@code "MCPE"}, or {@code "MCEE"} for Education Edition.
 */
public record BedrockStatusVersion(String name, int protocol, String brand)
    implements BaseStatusVersion {}
