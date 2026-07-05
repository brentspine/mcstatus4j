package de.brentspine.mcstatus4j.protocol.bedrock;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.io.ProtocolReadException;
import de.brentspine.mcstatus4j.responses.BedrockStatusResponse;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * The Bedrock edition status client: a single RakNet Unconnected Ping/Pong exchange over UDP.
 *
 * <p>Mirrors Python mcstatus's {@code BedrockClient}. Deliberately not built on the shared {@code
 * Buffer}/{@link de.brentspine.mcstatus4j.protocol.io.ProtocolReader} infrastructure used by the
 * other protocol clients - the Python source makes the same exception, working directly with raw
 * bytes for this one protocol. Also unlike the other clients, this one owns its socket directly
 * (one-shot request/response) rather than being handed an already-open {@code Connection}.
 */
public final class BedrockClient {

  private static final byte[] REQUEST_STATUS_DATA =
      HexFormat.of()
          .parseHex(
              "01" + "0000000000000000" + "00ffff00fefefefefdfdfdfd12345678" + "0000000000000000");

  private final Address address;
  private final Duration timeout;

  public BedrockClient(Address address, Duration timeout) {
    this.address = address;
    this.timeout = timeout;
  }

  /**
   * Parse a raw Unconnected Pong packet into a {@link BedrockStatusResponse}.
   *
   * <p>Strips the leading packet-id byte, reads a big-endian {@code USHORT} at offset 32-33 (of the
   * stripped data) as the server-name-string length, then decodes and semicolon-splits that many
   * UTF-8 bytes starting at offset 34.
   */
  public static BedrockStatusResponse parseResponse(byte[] rawData, double latency) {
    byte[] data = Arrays.copyOfRange(rawData, 1, rawData.length);
    int nameLength = ((data[32] & 0xFF) << 8) | (data[33] & 0xFF);
    String decoded = new String(data, 34, nameLength, StandardCharsets.UTF_8);

    List<Object> decodedData = new ArrayList<>(Arrays.asList(decoded.split(";", -1)));
    return BedrockStatusResponse.build(decodedData, latency);
  }

  /** Send the Unconnected Ping and read/parse the Unconnected Pong response. */
  public BedrockStatusResponse readStatus() {
    long start = System.nanoTime();
    byte[] data = readStatusRaw();
    long end = System.nanoTime();
    return parseResponse(data, (end - start) / 1_000_000.0);
  }

  private byte[] readStatusRaw() {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(toMillis(timeout));

      InetSocketAddress target = new InetSocketAddress(address.host(), address.port());
      socket.send(new DatagramPacket(REQUEST_STATUS_DATA, REQUEST_STATUS_DATA.length, target));

      byte[] buffer = new byte[2048];
      DatagramPacket response = new DatagramPacket(buffer, buffer.length);
      socket.receive(response);
      return Arrays.copyOf(response.getData(), response.getLength());
    } catch (IOException e) {
      throw new ProtocolReadException("Server did not respond with any information!", e);
    }
  }

  private static int toMillis(Duration timeout) {
    long millis = timeout.toMillis();
    return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
  }
}
