package de.brentspine.mcstatus4j.protocol.java;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.brentspine.mcstatus4j.net.Address;
import de.brentspine.mcstatus4j.protocol.io.FakeConnection;
import de.brentspine.mcstatus4j.protocol.io.ProtocolReadException;
import de.brentspine.mcstatus4j.responses.JavaStatusResponse;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/protocol/test_java_client.py}. */
class JavaClientTest {

  private static final HexFormat HEX = HexFormat.of();

  private FakeConnection connection;
  private JavaClient javaClient;

  @BeforeEach
  void setUp() {
    connection = new FakeConnection();
    javaClient = new JavaClient(connection, new Address("localhost", 25565), 44);
  }

  private static byte[] hex(String hexString) {
    return HEX.parseHex(hexString);
  }

  @Test
  void handshake() {
    javaClient.handshake();
    assertArrayEquals(hex("0F002C096C6F63616C686F737463DD01"), connection.flush());
  }

  @Test
  void readStatus() {
    connection.receive(
        hex(
            "7200707B226465736372697074696F6E223A2241204D696E65637261667420536572766572222C22706C6179657273223A7B2"
                + "26D6178223A32302C226F6E6C696E65223A307D2C2276657273696F6E223A7B226E616D65223A22312E382D70726531222C22"
                + "70726F746F636F6C223A34347D7D"));

    JavaStatusResponse status = javaClient.readStatus();

    assertEquals("A Minecraft Server", status.raw().get("description"));
    assertEquals(java.util.Map.of("max", 20, "online", 0), status.raw().get("players"));
    assertEquals(java.util.Map.of("name", "1.8-pre1", "protocol", 44), status.raw().get("version"));
    assertArrayEquals(hex("0100"), connection.flush());
  }

  @Test
  void readStatusInvalidJson() {
    connection.receive(hex("0300017B"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> javaClient.readStatus());
    assertEquals("Received invalid JSON", ex.getMessage());
  }

  @Test
  void readStatusToleratesMissingMotd() {
    // No "description" field (see mcstatus issue #922) - JavaStatusResponse.build defaults it.
    connection.receive(
        hex(
            "4F004D7B22706C6179657273223A7B226D6178223A32302C226F6E6C696E65223A307D2C2276657273696F6E223A7B226E616"
                + "D65223A22312E382D70726531222C2270726F746F636F6C223A34347D7D"));

    JavaStatusResponse status = javaClient.readStatus();
    assertEquals("", status.motd().toMinecraft());
  }

  @Test
  void readStatusInvalidStatusPacket() {
    connection.receive(hex("0105"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> javaClient.readStatus());
    assertEquals("Received invalid status response packet.", ex.getMessage());
  }

  @Test
  void testPing() {
    connection.receive(hex("09010000000000DD7D1C"));
    javaClient = new JavaClient(connection, new Address("localhost", 25565), 44, 14515484L);

    assertTrue(javaClient.testPing() >= 0);
    assertArrayEquals(hex("09010000000000DD7D1C"), connection.flush());
  }

  @Test
  void testPingInvalidPacket() {
    connection.receive(hex("011F"));
    javaClient = new JavaClient(connection, new Address("localhost", 25565), 44, 14515484L);

    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> javaClient.testPing());
    assertEquals("Received invalid ping response packet.", ex.getMessage());
  }

  @Test
  void testPingWrongToken() {
    connection.receive(hex("09010000000000DD7D1C"));
    javaClient = new JavaClient(connection, new Address("localhost", 25565), 44, 12345L);

    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> javaClient.testPing());
    assertEquals(
        "Received mangled ping response (expected token 12345, got 14515484)", ex.getMessage());
  }

  @Test
  void pingTokenDefaultsToRandomNonNegativeValue() {
    JavaClient client = new JavaClient(connection, new Address("localhost", 25565), 44);
    assertTrue(client.pingToken() >= 0);
  }
}
