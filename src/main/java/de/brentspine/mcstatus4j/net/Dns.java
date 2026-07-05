package de.brentspine.mcstatus4j.net;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Thin wrapper over dnsjava for the A-record and SRV-record lookups mcstatus4j needs.
 *
 * <p>Mirrors Python mcstatus's {@code _net/dns.py} (built on dnspython). Every method accepts an
 * optional {@link Resolver} override so tests can inject a fake resolver instead of hitting real
 * DNS; passing {@code null} uses dnsjava's configured default resolver.
 */
public final class Dns {

  private Dns() {}

  /**
   * The resolved target host (with any trailing root-zone dot stripped) and port of an SRV record.
   */
  public record SrvTarget(String host, int port) {}

  /**
   * Resolve the A record for {@code hostname}.
   *
   * @throws IOException if the name is invalid, or the lookup doesn't succeed (including "host not
   *     found").
   */
  public static InetAddress resolveARecord(String hostname) throws IOException {
    return resolveARecord(hostname, null);
  }

  /** As {@link #resolveARecord(String)}, but resolving through the given {@code resolver}. */
  public static InetAddress resolveARecord(String hostname, Resolver resolver) throws IOException {
    Lookup lookup = newLookup(hostname, Type.A, resolver);
    Record[] records = lookup.run();
    requireSuccessful(lookup, hostname);
    return ((ARecord) records[0]).getAddress();
  }

  /**
   * Resolve the SRV record for {@code queryName}.
   *
   * @return the resolved target, or {@link Optional#empty()} if no such record exists (the DNS
   *     equivalent of dnspython's {@code NXDOMAIN}/{@code NoAnswer}).
   * @throws IOException if the name is invalid, or the lookup fails for any other reason.
   */
  public static Optional<SrvTarget> resolveSrvRecord(String queryName) throws IOException {
    return resolveSrvRecord(queryName, null);
  }

  /** As {@link #resolveSrvRecord(String)}, but resolving through the given {@code resolver}. */
  public static Optional<SrvTarget> resolveSrvRecord(String queryName, Resolver resolver)
      throws IOException {
    Lookup lookup = newLookup(queryName, Type.SRV, resolver);
    Record[] records = lookup.run();

    int result = lookup.getResult();
    if (result == Lookup.HOST_NOT_FOUND || result == Lookup.TYPE_NOT_FOUND) {
      return Optional.empty();
    }
    requireSuccessful(lookup, queryName);

    SRVRecord srv = (SRVRecord) records[0];
    return Optional.of(new SrvTarget(stripTrailingDot(srv.getTarget().toString()), srv.getPort()));
  }

  /**
   * Resolve the SRV record for the standard Minecraft Java edition service name, {@code
   * _minecraft._tcp.<hostname>}.
   */
  public static Optional<SrvTarget> resolveMcSrv(String hostname) throws IOException {
    return resolveSrvRecord("_minecraft._tcp." + hostname);
  }

  /** As {@link #resolveMcSrv(String)}, but resolving through the given {@code resolver}. */
  public static Optional<SrvTarget> resolveMcSrv(String hostname, Resolver resolver)
      throws IOException {
    return resolveSrvRecord("_minecraft._tcp." + hostname, resolver);
  }

  private static Lookup newLookup(String name, int type, Resolver resolver) throws IOException {
    Lookup lookup;
    try {
      lookup = new Lookup(name, type);
    } catch (TextParseException e) {
      throw new IOException("Invalid DNS name: '" + name + "'", e);
    }
    if (resolver != null) {
      lookup.setResolver(resolver);
    }
    // dnsjava's Lookup consults a shared, process-global default cache unless given its own; we
    // don't want implicit unbounded caching of DNS answers hidden inside the library (matching
    // dnspython, which mcstatus doesn't layer any persistent cache on top of either), and a shared
    // cache would also make repeated lookups against the same name in tests non-hermetic.
    lookup.setCache(new Cache());
    return lookup;
  }

  private static void requireSuccessful(Lookup lookup, String name) throws IOException {
    if (lookup.getResult() != Lookup.SUCCESSFUL) {
      throw new IOException("DNS lookup for '" + name + "' failed: " + lookup.getErrorString());
    }
  }

  private static String stripTrailingDot(String value) {
    return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
  }
}
