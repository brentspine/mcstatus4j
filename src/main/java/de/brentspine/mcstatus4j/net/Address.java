package de.brentspine.mcstatus4j.net;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import org.xbill.DNS.Resolver;

/**
 * A validated {@code (host, port)} pair, with support for parsing {@code "host:port"} strings and
 * resolving the host into an {@link InetAddress}.
 *
 * <p>Mirrors Python mcstatus's {@code Address}. Unlike the response DTOs elsewhere in this port,
 * this stays a mutable class rather than a record: {@link #resolveIp()} memoizes its result (a
 * {@code _cached_ip}-equivalent field), the same reason {@link
 * de.brentspine.mcstatus4j.protocol.io.Buffer} isn't a record either.
 */
public final class Address {

  private static final System.Logger LOGGER = System.getLogger(Address.class.getName());

  private final String host;
  private final int port;
  private volatile InetAddress cachedIp;

  public Address(String host, int port) {
    ensureValidity(host, port);
    this.host = host;
    this.port = port;
  }

  static void ensureValidity(String host, int port) {
    if (host == null) {
      throw new IllegalArgumentException("Host must be a non-null string address");
    }
    if (port > 65535 || port < 0) {
      throw new IllegalArgumentException(
          "Port must be within the allowed range (0-65535), got " + port);
    }
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public static Address fromTuple(String host, int port) {
    return new Address(host, port);
  }

  /**
   * Parse a {@code "host:port"} string, requiring the address to include a port.
   *
   * @throws IllegalArgumentException if the address can't be parsed, or lacks a port.
   */
  public static Address parseAddress(String address) {
    return parseAddress(address, null);
  }

  /**
   * Parse a {@code "host:port"} string like {@code "127.0.0.1:25565"}. If the address doesn't
   * include a port, {@code defaultPort} is used instead.
   *
   * @throws IllegalArgumentException if the address can't be parsed, or lacks a port and {@code
   *     defaultPort} is {@code null}.
   */
  public static Address parseAddress(String address, Integer defaultPort) {
    ParsedHostPort parsed = parseHostPort(address);
    Integer port = parsed.port();
    if (port == null) {
      if (defaultPort == null) {
        throw new IllegalArgumentException(
            "Given address '"
                + address
                + "' doesn't contain port and default_port wasn't specified, can't parse.");
      }
      port = defaultPort;
    }
    return new Address(parsed.host(), port);
  }

  /**
   * Resolve this address's host into an {@link InetAddress}, using dnsjava's default resolver.
   *
   * <p>If the host is already an IP literal, this is resolved locally without any DNS traffic. The
   * result is cached after the first successful resolution.
   */
  public InetAddress resolveIp() throws IOException {
    return resolveIp(null);
  }

  /**
   * As {@link #resolveIp()}, but resolving A records (if needed) through the given {@code
   * resolver}.
   */
  public InetAddress resolveIp(Resolver resolver) throws IOException {
    InetAddress cached = cachedIp;
    if (cached != null) {
      return cached;
    }

    String resolveHost = host;
    if ("localhost".equals(host) && isMacOs()) {
      resolveHost = "127.0.0.1";
      LOGGER.log(
          Level.WARNING,
          "On macOS, 'localhost' cannot always be resolved reliably; treating it as 127.0.0.1. Consider"
              + " using '127.0.0.1' (or '::1' for IPv6) directly instead.");
    }

    InetAddress ip;
    try {
      ip = org.xbill.DNS.Address.getByAddress(resolveHost);
    } catch (UnknownHostException notALiteral) {
      ip = Dns.resolveARecord(resolveHost, resolver);
    }

    cachedIp = ip;
    return ip;
  }

  private static boolean isMacOs() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Address other)) {
      return false;
    }
    return port == other.port && host.equals(other.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }

  @Override
  public String toString() {
    return "Address[host=" + host + ", port=" + port + "]";
  }

  /** Package-private so {@link AddressResolver} can reuse the same {@code "host:port"} parser. */
  record ParsedHostPort(String host, Integer port) {}

  private static final String RESERVED_HOST_CHARS = "@#/?[] \t\n\r";

  static ParsedHostPort parseHostPort(String address) {
    if (address.isEmpty()) {
      throw invalidAddress(address);
    }

    String host;
    String portString = null;

    if (address.startsWith("[")) {
      int closeBracket = address.indexOf(']');
      if (closeBracket < 0) {
        throw invalidAddress(address);
      }
      host = address.substring(1, closeBracket);
      String rest = address.substring(closeBracket + 1);
      if (!rest.isEmpty()) {
        if (!rest.startsWith(":")) {
          throw invalidAddress(address);
        }
        portString = rest.substring(1);
      }
    } else {
      int firstColon = address.indexOf(':');
      if (firstColon < 0) {
        host = address;
      } else {
        host = address.substring(0, firstColon);
        portString = address.substring(firstColon + 1);
      }
    }

    if (host.isEmpty() || containsReservedChar(host)) {
      throw invalidAddress(address);
    }

    Integer port = null;
    if (portString != null) {
      try {
        port = Integer.parseInt(portString.trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Port could not be cast to integer value as '" + portString + "'");
      }
    }

    return new ParsedHostPort(host, port);
  }

  private static boolean containsReservedChar(String host) {
    for (int i = 0; i < host.length(); i++) {
      if (RESERVED_HOST_CHARS.indexOf(host.charAt(i)) >= 0) {
        return true;
      }
    }
    return false;
  }

  private static IllegalArgumentException invalidAddress(String address) {
    return new IllegalArgumentException("Invalid address '" + address + "', can't parse.");
  }
}
