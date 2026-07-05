package de.brentspine.mcstatus4j;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.net.AddressResolver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

/**
 * Base class for a Minecraft Java edition server ({@link JavaServer} and {@link LegacyServer}),
 * adding SRV-record-aware address resolution shared by both.
 *
 * <p>Mirrors Python mcstatus's {@code BaseJavaServer}.
 */
public abstract class BaseJavaServer extends McServer {

  public static final int DEFAULT_PORT = 25565;

  protected BaseJavaServer(String host, int port, Duration timeout) {
    super(host, port, timeout);
  }

  /**
   * Mimics how the vanilla Minecraft Java client resolves the address a user types in: parses
   * {@code "host:port"}, and if no port is given, looks up the {@code _minecraft._tcp.<host>} SRV
   * record before falling back to {@link #DEFAULT_PORT}.
   */
  protected static Address lookupAddress(String address) {
    try {
      return AddressResolver.lookupJavaAddress(address, DEFAULT_PORT);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
