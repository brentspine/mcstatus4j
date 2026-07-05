package de.brentspine.mcstatus4j;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.io.TcpConnection;
import de.brentspine.mcstatus4j.protocol.legacy.LegacyClient;
import de.brentspine.mcstatus4j.responses.LegacyStatusResponse;
import de.brentspine.mcstatus4j.util.Retry;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * A pre-1.7 Minecraft Java edition server.
 *
 * <p>Mirrors Python mcstatus's {@code LegacyServer}. Unlike {@link JavaServer}, a fresh TCP
 * connection is opened on every retry attempt (matching Python: the whole {@code status()} method
 * is retried, not just the client logic over one already-open connection).
 */
public final class LegacyServer extends BaseJavaServer {

  public LegacyServer(String host) {
    this(host, DEFAULT_PORT, McServer.defaultTimeout());
  }

  public LegacyServer(String host, int port) {
    this(host, port, McServer.defaultTimeout());
  }

  public LegacyServer(String host, int port, Duration timeout) {
    super(host, port, timeout);
  }

  public static LegacyServer lookup(String address) {
    return lookup(address, McServer.defaultTimeout());
  }

  public static LegacyServer lookup(String address, Duration timeout) {
    Address addr = lookupAddress(address);
    return new LegacyServer(addr.host(), addr.port(), timeout);
  }

  /** Check the status of this pre-1.7 server. */
  public LegacyStatusResponse status() {
    return status(3);
  }

  public LegacyStatusResponse status(int tries) {
    return Retry.call(
        tries,
        () -> {
          try (TcpConnection connection =
              new TcpConnection(new InetSocketAddress(address.host(), address.port()), timeout)) {
            return new LegacyClient(connection).readStatus();
          }
        });
  }

  public CompletableFuture<LegacyStatusResponse> statusAsync() {
    return statusAsync(3);
  }

  public CompletableFuture<LegacyStatusResponse> statusAsync(int tries) {
    return CompletableFuture.supplyAsync(() -> status(tries), DEFAULT_ASYNC_EXECUTOR);
  }
}
