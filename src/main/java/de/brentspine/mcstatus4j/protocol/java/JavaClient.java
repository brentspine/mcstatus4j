package de.brentspine.mcstatus4j.protocol.java;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.io.Buffer;
import de.brentspine.mcstatus4j.protocol.io.Connection;
import de.brentspine.mcstatus4j.protocol.io.ProtocolReadException;
import de.brentspine.mcstatus4j.responses.JavaStatusResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The modern (1.7+) Java edition Server List Ping client: handshake, status request, and ping.
 *
 * <p>Mirrors Python mcstatus's {@code _BaseJavaClient}/{@code JavaClient}, collapsed into a single
 * blocking class - see {@link Connection}'s class docs for why there's no separate async client
 * here.
 */
public final class JavaClient {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final Connection connection;
  private final Address address;
  private final int version;
  private final long pingToken;

  public JavaClient(Connection connection, Address address, int version) {
    this(connection, address, version, randomPingToken());
  }

  public JavaClient(Connection connection, Address address, int version, long pingToken) {
    this.connection = connection;
    this.address = address;
    this.version = version;
    this.pingToken = pingToken;
  }

  private static long randomPingToken() {
    // Mirrors Python's random.randint(0, (1 << 63) - 1): a uniformly random non-negative 63-bit
    // value. Masking a uniformly random 64-bit long with Long.MAX_VALUE clears just the sign bit.
    return ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE;
  }

  public long pingToken() {
    return pingToken;
  }

  /** Write the initial handshake packet, declaring intent to request status. */
  public void handshake() {
    connection.writeByteArray(buildHandshakePacket().flush());
  }

  private Buffer buildHandshakePacket() {
    Buffer packet = new Buffer();
    packet.writeVarint(0);
    packet.writeVarint(version);
    packet.writeUtf(address.host());
    packet.writeUnsignedShort(address.port());
    packet.writeVarint(1); // Intention to query status.
    return packet;
  }

  /** Send the status request and read/parse the response. */
  public JavaStatusResponse readStatus() {
    Buffer request = new Buffer();
    request.writeVarint(0); // Request status.
    connection.writeByteArray(request.flush());

    long start = System.nanoTime();
    Buffer response = new Buffer(connection.readByteArray());
    long end = System.nanoTime();

    return handleStatusResponse(response, start, end);
  }

  /** Send a ping token and measure the round-trip latency. */
  public double testPing() {
    Buffer request = new Buffer();
    request.writeVarint(1); // Test ping.
    request.writeLongLong(pingToken);

    long start = System.nanoTime();
    connection.writeByteArray(request.flush());
    Buffer response = new Buffer(connection.readByteArray());
    long end = System.nanoTime();

    return handlePingResponse(response, start, end);
  }

  private JavaStatusResponse handleStatusResponse(Buffer response, long startNanos, long endNanos) {
    if (response.readVarint() != 0) {
      throw new ProtocolReadException("Received invalid status response packet.");
    }

    Map<String, Object> raw;
    try {
      raw = JSON.readValue(response.readUtf(), new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      throw new ProtocolReadException("Received invalid JSON", e);
    }

    double latencyMs = (endNanos - startNanos) / 1_000_000.0;
    try {
      return JavaStatusResponse.build(raw, latencyMs);
    } catch (RuntimeException e) {
      throw new ProtocolReadException("Received invalid status response", e);
    }
  }

  private double handlePingResponse(Buffer response, long startNanos, long endNanos) {
    if (response.readVarint() != 1) {
      throw new ProtocolReadException("Received invalid ping response packet.");
    }
    long receivedToken = response.readLongLong();
    if (receivedToken != pingToken) {
      throw new ProtocolReadException(
          "Received mangled ping response (expected token "
              + pingToken
              + ", got "
              + receivedToken
              + ")");
    }
    return (endNanos - startNanos) / 1_000_000.0;
  }
}
