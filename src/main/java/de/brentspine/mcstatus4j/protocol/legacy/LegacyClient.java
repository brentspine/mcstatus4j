package de.brentspine.mcstatus4j.protocol.legacy;

import de.brentspine.mcstatus4j.protocol.io.Connection;
import de.brentspine.mcstatus4j.protocol.io.ProtocolReadException;
import de.brentspine.mcstatus4j.responses.LegacyStatusResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The pre-1.7 (legacy) Java edition Server List Ping client.
 *
 * <p>Mirrors Python mcstatus's {@code _BaseLegacyClient}/{@code LegacyClient}, collapsed into a
 * single blocking class - see {@link Connection}'s class docs for why.
 */
public final class LegacyClient {

  private static final byte[] REQUEST_STATUS_DATA = {(byte) 0xFE, 0x01, (byte) 0xFA};
  private static final String NUL = String.valueOf((char) 0);
  private static final String SECTION_SIGN = "§";

  private final Connection connection;

  public LegacyClient(Connection connection) {
    this.connection = connection;
  }

  /** Send the status request and read/parse the response. */
  public LegacyStatusResponse readStatus() {
    long start = System.nanoTime();
    connection.write(REQUEST_STATUS_DATA);

    byte[] id = connection.read(1);
    if (id[0] != (byte) 0xFF) {
      throw new ProtocolReadException("Received invalid packet ID");
    }
    int length = connection.readUnsignedShort();
    byte[] data = connection.read(length * 2);

    long end = System.nanoTime();
    return parseResponse(data, (end - start) / 1_000_000.0);
  }

  /**
   * Parse a decoded UTF-16BE kick-packet payload into a {@link LegacyStatusResponse}.
   *
   * <p>Servers older than 1.4 (12w42a) send a kick packet that lacks the section-sign-1 marker and
   * protocol/version fields entirely; that case is reconstructed into the same 6-field shape with
   * sentinel values ({@code protocol=-1}, {@code name="<1.4"}).
   *
   * @throws ProtocolReadException if the reconstructed pre-1.4 fallback still doesn't have exactly
   *     6 fields.
   */
  public static LegacyStatusResponse parseResponse(byte[] data, double latency) {
    String decoded = new String(data, StandardCharsets.UTF_16BE);
    List<String> decodedData = new ArrayList<>(List.of(decoded.split(NUL, -1)));

    if (!(SECTION_SIGN + "1").equals(decodedData.get(0))) {
      List<String> reconstructed = new ArrayList<>(List.of(SECTION_SIGN + "1", "-1", "<1.4"));
      reconstructed.addAll(List.of(decodedData.get(0).split(SECTION_SIGN, -1)));
      if (reconstructed.size() != 6) {
        throw new ProtocolReadException("Received invalid kick packet reason");
      }
      decodedData = reconstructed;
    }

    return LegacyStatusResponse.build(decodedData.subList(1, decodedData.size()), latency);
  }
}
