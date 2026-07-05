package de.brentspine.mcstatus4j;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.bedrock.BedrockClient;
import de.brentspine.mcstatus4j.responses.BedrockStatusResponse;
import de.brentspine.mcstatus4j.util.Retry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * A Minecraft Bedrock edition server.
 *
 * <p>Mirrors Python mcstatus's {@code BedrockServer}. No SRV-record lookup (Bedrock doesn't use
 * them) and no query protocol support (Query is Java-only).
 */
public final class BedrockServer extends McServer {

  public static final int DEFAULT_PORT = 19132;

  public BedrockServer(String host) {
    this(host, DEFAULT_PORT, McServer.defaultTimeout());
  }

  public BedrockServer(String host, int port) {
    this(host, port, McServer.defaultTimeout());
  }

  public BedrockServer(String host, int port, Duration timeout) {
    super(host, port, timeout);
  }

  public static BedrockServer lookup(String address) {
    return lookup(address, McServer.defaultTimeout());
  }

  public static BedrockServer lookup(String address, Duration timeout) {
    Address addr = Address.parseAddress(address, DEFAULT_PORT);
    return new BedrockServer(addr.host(), addr.port(), timeout);
  }

  /** Check the status of this Bedrock server. */
  public BedrockStatusResponse status() {
    return status(3);
  }

  public BedrockStatusResponse status(int tries) {
    return Retry.call(tries, () -> new BedrockClient(address, timeout).readStatus());
  }

  public CompletableFuture<BedrockStatusResponse> statusAsync() {
    return statusAsync(3);
  }

  public CompletableFuture<BedrockStatusResponse> statusAsync(int tries) {
    return CompletableFuture.supplyAsync(() -> status(tries), DEFAULT_ASYNC_EXECUTOR);
  }
}
