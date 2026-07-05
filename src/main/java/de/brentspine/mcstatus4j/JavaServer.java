package de.brentspine.mcstatus4j;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.io.TcpConnection;
import de.brentspine.mcstatus4j.protocol.io.UdpConnection;
import de.brentspine.mcstatus4j.protocol.java.JavaClient;
import de.brentspine.mcstatus4j.protocol.query.QueryClient;
import de.brentspine.mcstatus4j.responses.JavaStatusResponse;
import de.brentspine.mcstatus4j.responses.QueryResponse;
import de.brentspine.mcstatus4j.util.Retry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * A 1.7+ Minecraft Java edition server: status, ping, and query (GameSpy4).
 *
 * <p>Mirrors Python mcstatus's {@code JavaServer}. Every blocking method has an {@code *Async}
 * counterpart returning a {@link CompletableFuture}, backed by {@link #DEFAULT_ASYNC_EXECUTOR}
 * (virtual threads) rather than a hand-duplicated non-blocking implementation.
 */
public final class JavaServer extends BaseJavaServer {

  private final int queryPort;

  public JavaServer(String host) {
    this(host, DEFAULT_PORT, McServer.defaultTimeout(), null);
  }

  public JavaServer(String host, int port) {
    this(host, port, McServer.defaultTimeout(), null);
  }

  public JavaServer(String host, int port, Duration timeout) {
    this(host, port, timeout, null);
  }

  /**
   * @param queryPort typically the same as {@code port}, but can differ; defaults to {@code port}
   *     if {@code null}.
   */
  public JavaServer(String host, int port, Duration timeout, Integer queryPort) {
    super(host, port, timeout);
    this.queryPort = queryPort != null ? queryPort : port;
    new Address(host, this.queryPort); // Ensure queryPort is valid.
  }

  public int queryPort() {
    return queryPort;
  }

  /** Mimics Minecraft's server address field, including SRV record resolution. */
  public static JavaServer lookup(String address) {
    return lookup(address, McServer.defaultTimeout());
  }

  /** As {@link #lookup(String)}, with an explicit connection timeout. */
  public static JavaServer lookup(String address, Duration timeout) {
    Address addr = lookupAddress(address);
    return new JavaServer(addr.host(), addr.port(), timeout);
  }

  private TcpConnection openTcp() {
    return new TcpConnection(new InetSocketAddress(address.host(), address.port()), timeout);
  }

  private JavaClient newClient(TcpConnection connection, int version, Long pingToken) {
    return pingToken != null
        ? new JavaClient(connection, address, version, pingToken)
        : new JavaClient(connection, address, version);
  }

  /**
   * Check the latency between this server and the client (you).
   *
   * <p>Note that most non-vanilla implementations fail to respond to a ping packet unless a status
   * packet is sent first - expect a {@code ProtocolReadException} in those cases. The workaround is
   * to use the latency from {@link #status()} as the ping time instead.
   */
  public double ping() {
    return ping(3, 47, null);
  }

  public double ping(int tries, int version, Long pingToken) {
    try (TcpConnection connection = openTcp()) {
      return Retry.call(
          tries,
          () -> {
            JavaClient client = newClient(connection, version, pingToken);
            client.handshake();
            return client.testPing();
          });
    }
  }

  public CompletableFuture<Double> pingAsync() {
    return pingAsync(3, 47, null);
  }

  public CompletableFuture<Double> pingAsync(int tries, int version, Long pingToken) {
    return CompletableFuture.supplyAsync(
        () -> ping(tries, version, pingToken), DEFAULT_ASYNC_EXECUTOR);
  }

  /** Check this server's status via the Server List Ping status protocol. */
  public JavaStatusResponse status() {
    return status(3, 47, null);
  }

  public JavaStatusResponse status(int tries, int version, Long pingToken) {
    try (TcpConnection connection = openTcp()) {
      return Retry.call(
          tries,
          () -> {
            JavaClient client = newClient(connection, version, pingToken);
            client.handshake();
            return client.readStatus();
          });
    }
  }

  public CompletableFuture<JavaStatusResponse> statusAsync() {
    return statusAsync(3, 47, null);
  }

  public CompletableFuture<JavaStatusResponse> statusAsync(int tries, int version, Long pingToken) {
    return CompletableFuture.supplyAsync(
        () -> status(tries, version, pingToken), DEFAULT_ASYNC_EXECUTOR);
  }

  /** Check this server's status via the GameSpy4 Query protocol (must have query enabled). */
  public QueryResponse query() {
    return query(3);
  }

  public QueryResponse query(int tries) {
    String ip;
    try {
      ip = address.resolveIp().getHostAddress();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Address queryAddr = new Address(ip, queryPort);

    return Retry.call(
        tries,
        () -> {
          try (UdpConnection connection =
              new UdpConnection(
                  new InetSocketAddress(queryAddr.host(), queryAddr.port()), timeout)) {
            QueryClient client = new QueryClient(connection);
            client.handshake();
            return client.readQuery();
          }
        });
  }

  public CompletableFuture<QueryResponse> queryAsync() {
    return queryAsync(3);
  }

  public CompletableFuture<QueryResponse> queryAsync(int tries) {
    return CompletableFuture.supplyAsync(() -> query(tries), DEFAULT_ASYNC_EXECUTOR);
  }
}
