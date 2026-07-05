package de.brentspine.mcstatus4j;

import de.brentspine.mcstatus4j.net.Address;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Base class for a general Minecraft server. Contains only the logic shared across Java and Bedrock
 * editions - it doesn't include any version-specific settings and can't make requests itself.
 *
 * <p>Mirrors Python mcstatus's {@code MCServer}.
 */
public abstract class McServer {

  /**
   * Executor backing every {@code *Async} method across the {@code *Server} classes: cheap
   * thread-per-call execution of the (single, blocking) protocol implementation, rather than a
   * hand-duplicated non-blocking protocol stack. See {@link
   * de.brentspine.mcstatus4j.protocol.io.Connection}'s class docs for the full rationale.
   */
  protected static final Executor DEFAULT_ASYNC_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

  protected final Address address;
  protected final Duration timeout;

  protected McServer(String host, int port, Duration timeout) {
    this.address = new Address(host, port);
    this.timeout = timeout;
  }

  public static Duration defaultTimeout() {
    return DEFAULT_TIMEOUT;
  }

  public Address address() {
    return address;
  }

  public Duration timeout() {
    return timeout;
  }
}
