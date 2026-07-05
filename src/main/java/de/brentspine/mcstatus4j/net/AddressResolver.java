package de.brentspine.mcstatus4j.net;

import java.io.IOException;
import java.util.Optional;
import org.xbill.DNS.Resolver;

/**
 * SRV-record-aware address resolution mimicking how the vanilla Minecraft Java client resolves the
 * address a user types in: if the given address doesn't specify a port, look up the {@code
 * _minecraft._tcp.<host>} SRV record for one before falling back to a default port.
 *
 * <p>Mirrors Python mcstatus's {@code minecraft_srv_address_lookup}/{@code
 * async_minecraft_srv_address_lookup} in {@code _net/address.py}, collapsed to a single blocking
 * method per mcstatus4j's concurrency model.
 */
public final class AddressResolver {

  private AddressResolver() {}

  /**
   * @throws IllegalArgumentException if the address can't be parsed, or lacks a port, has no SRV
   *     record, and {@code defaultPort} is {@code null}.
   * @throws IOException if the SRV lookup itself fails (for a reason other than "no such record").
   */
  public static Address lookupJavaAddress(String address, Integer defaultPort) throws IOException {
    return lookupJavaAddress(address, defaultPort, null);
  }

  /**
   * As {@link #lookupJavaAddress(String, Integer)}, but resolving through the given {@code
   * resolver}.
   */
  public static Address lookupJavaAddress(String address, Integer defaultPort, Resolver resolver)
      throws IOException {
    Address.ParsedHostPort parsed = Address.parseHostPort(address);

    if (parsed.port() != null) {
      return new Address(parsed.host(), parsed.port());
    }

    Optional<Dns.SrvTarget> srv = Dns.resolveMcSrv(parsed.host(), resolver);
    if (srv.isPresent()) {
      return new Address(srv.get().host(), srv.get().port());
    }

    if (defaultPort == null) {
      throw new IllegalArgumentException(
          "Given address '"
              + address
              + "' doesn't contain port, doesn't have an SRV record pointing to a port, and default_port wasn't"
              + " specified, can't parse.");
    }
    return new Address(parsed.host(), defaultPort);
  }
}
