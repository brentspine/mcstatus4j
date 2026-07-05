package de.brentspine.mcstatus4j.protocol.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.brentspine.mcstatus4j.protocol.io.FakeConnection;
import de.brentspine.mcstatus4j.protocol.io.ProtocolReadException;
import de.brentspine.mcstatus4j.responses.LegacyStatusResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/protocol/test_legacy_client.py}. */
class LegacyClientTest {

  private static final HexFormat HEX = HexFormat.of();
  private static final String SECTION_SIGN = String.valueOf((char) 0x00A7);
  private static final String NUL = String.valueOf((char) 0);

  @Test
  void invalidKickReason() {
    byte[] data = "Invalid Reason".getBytes(StandardCharsets.UTF_16BE);
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> LegacyClient.parseResponse(data, 123.0));
    assertEquals("Received invalid kick packet reason", ex.getMessage());
  }

  @Test
  void parseResponsePre14() {
    String response = "A Minecraft Server" + SECTION_SIGN + "0" + SECTION_SIGN + "20";
    LegacyStatusResponse result =
        LegacyClient.parseResponse(response.getBytes(StandardCharsets.UTF_16BE), 123.0);

    assertEquals(0, result.players().online());
    assertEquals(20, result.players().max());
    assertEquals("<1.4", result.version().name());
    assertEquals(-1, result.version().protocol());
    assertEquals("A Minecraft Server", result.motd().toMinecraft());
    assertEquals(123.0, result.latency());
  }

  @Test
  void parseResponse147() {
    String response =
        SECTION_SIGN
            + "1"
            + NUL
            + "51"
            + NUL
            + "1.4.7"
            + NUL
            + "A Minecraft Server"
            + NUL
            + "0"
            + NUL
            + "20";
    LegacyStatusResponse result =
        LegacyClient.parseResponse(response.getBytes(StandardCharsets.UTF_16BE), 123.0);

    assertEquals(0, result.players().online());
    assertEquals(20, result.players().max());
    assertEquals("1.4.7", result.version().name());
    assertEquals(51, result.version().protocol());
    assertEquals("A Minecraft Server", result.motd().toMinecraft());
    assertEquals(123.0, result.latency());
  }

  @Test
  void invalidPacketId() {
    FakeConnection connection = new FakeConnection();
    connection.receive(HEX.parseHex("00"));
    LegacyClient client = new LegacyClient(connection);

    ProtocolReadException ex = assertThrows(ProtocolReadException.class, client::readStatus);
    assertEquals("Received invalid packet ID", ex.getMessage());
  }

  @Test
  void readStatusHappyPath() {
    FakeConnection connection = new FakeConnection();
    String response =
        SECTION_SIGN
            + "1"
            + NUL
            + "51"
            + NUL
            + "1.4.7"
            + NUL
            + "A Minecraft Server"
            + NUL
            + "0"
            + NUL
            + "20";
    byte[] payload = response.getBytes(StandardCharsets.UTF_16BE);

    int charCount = payload.length / 2;
    connection.receive(new byte[] {(byte) 0xFF});
    connection.receive(new byte[] {(byte) (charCount >>> 8), (byte) charCount});
    connection.receive(payload);

    LegacyClient client = new LegacyClient(connection);
    LegacyStatusResponse result = client.readStatus();

    assertEquals(51, result.version().protocol());
    assertEquals(
        HEX.formatHex(new byte[] {(byte) 0xFE, 0x01, (byte) 0xFA}),
        HEX.formatHex(connection.flush()));
  }
}
