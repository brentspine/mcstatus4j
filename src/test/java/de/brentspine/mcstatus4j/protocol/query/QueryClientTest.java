package de.brentspine.mcstatus4j.protocol.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.brentspine.mcstatus4j.protocol.io.FakeDatagramConnection;
import de.brentspine.mcstatus4j.responses.QueryResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/protocol/test_query_client.py}. */
class QueryClientTest {

  private static final HexFormat HEX = HexFormat.of();
  private static final int NUL = 0;

  private FakeDatagramConnection connection;
  private QueryClient queryClient;

  @BeforeEach
  void setUp() {
    connection = new FakeDatagramConnection();
    queryClient = new QueryClient(connection);
  }

  private static byte[] hex(String hexString) {
    return HEX.parseHex(hexString);
  }

  /**
   * Builds a byte array from a mix of ASCII strings and raw byte values, avoiding embedding literal
   * control characters in source.
   */
  private static byte[] bytes(Object... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (Object part : parts) {
      if (part instanceof String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
      } else if (part instanceof Integer i) {
        out.write(i);
      } else {
        throw new IllegalArgumentException("Unsupported part: " + part);
      }
    }
    return out.toByteArray();
  }

  @Test
  void handshake() {
    connection.receive(hex("090000000035373033353037373800"));
    queryClient.handshake();

    byte[] sent = connection.flush();
    assertArrayStartsWith(hex("FEFD09"), sent);
    assertEquals(570350778, queryClient.challenge());
  }

  private static void assertArrayStartsWith(byte[] prefix, byte[] actual) {
    byte[] actualPrefix = java.util.Arrays.copyOf(actual, prefix.length);
    org.junit.jupiter.api.Assertions.assertArrayEquals(prefix, actualPrefix);
  }

  @Test
  void query() {
    connection.receive(
        hex(
            "00000000000000000000000000000000686f73746e616d650041204d696e656372616674205365727665720067616d6574797"
                + "06500534d500067616d655f6964004d494e4543524146540076657273696f6e00312e3800706c7567696e7300006d61700077"
                + "6f726c64006e756d706c61796572730033006d6178706c617965727300323000686f7374706f727400323535363500686f737"
                + "46970003139322e3136382e35362e31000001706c617965725f000044696e6e6572626f6e6500446a696e6e69626f6e650053"
                + "746576650000"));

    QueryResponse response = queryClient.readQuery();
    byte[] sent = connection.flush();

    assertArrayStartsWith(hex("FEFD00"), sent);
    assertEquals("A Minecraft Server", response.raw().get("hostname"));
    assertEquals("SMP", response.raw().get("gametype"));
    assertEquals("MINECRAFT", response.raw().get("game_id"));
    assertEquals("1.8", response.raw().get("version"));
    assertEquals("", response.raw().get("plugins"));
    assertEquals("world", response.raw().get("map"));
    assertEquals("3", response.raw().get("numplayers"));
    assertEquals("20", response.raw().get("maxplayers"));
    assertEquals("25565", response.raw().get("hostport"));
    assertEquals("192.168.56.1", response.raw().get("hostip"));
    assertEquals(java.util.List.of("Dinnerbone", "Djinnibone", "Steve"), response.players().list());
  }

  @Test
  void queryHandlesUnorderedMapResponse() {
    byte[] data =
        bytes(
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            "GeyserMC",
            NUL,
            0x80,
            NUL,
            "hostname",
            NUL,
            "Geyser",
            NUL,
            "hostip",
            NUL,
            "1.1.1.1",
            NUL,
            "plugins",
            NUL,
            NUL,
            "numplayers",
            NUL,
            "1",
            NUL,
            "gametype",
            NUL,
            "SMP",
            NUL,
            "maxplayers",
            NUL,
            "100",
            NUL,
            "hostport",
            NUL,
            "19132",
            NUL,
            "version",
            NUL,
            "Geyser (git-master-0fd903e) 1.18.10",
            NUL,
            "map",
            NUL,
            "Geyser",
            NUL,
            "game_id",
            NUL,
            "MINECRAFT",
            NUL,
            NUL,
            0x01,
            "player_",
            NUL,
            NUL,
            NUL);
    connection.receive(data);

    QueryResponse response = queryClient.readQuery();
    connection.flush();

    assertEquals("MINECRAFT", response.raw().get("game_id"));
    assertEquals("Geyser", response.motd().toMinecraft());
    assertEquals("Geyser (git-master-0fd903e) 1.18.10", response.software().version());
  }

  @Test
  void queryHandlesUnicodeMotdWithNulls() {
    byte[] data =
        bytes(
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            NUL,
            "hostname",
            NUL,
            NUL,
            "*",
            "K",
            0xD5,
            NUL,
            "gametype",
            NUL,
            "SMP",
            NUL,
            "game_id",
            NUL,
            "MINECRAFT",
            NUL,
            "version",
            NUL,
            "1.16.5",
            NUL,
            "plugins",
            NUL,
            "Paper on 1.16.5-R0.1-SNAPSHOT",
            NUL,
            "map",
            NUL,
            "world",
            NUL,
            "numplayers",
            NUL,
            "0",
            NUL,
            "maxplayers",
            NUL,
            "20",
            NUL,
            "hostport",
            NUL,
            "25565",
            NUL,
            "hostip",
            NUL,
            "127.0.1.1",
            NUL,
            NUL,
            0x01,
            "player_",
            NUL,
            NUL,
            NUL);
    connection.receive(data);

    QueryResponse response = queryClient.readQuery();
    connection.flush();

    assertEquals("MINECRAFT", response.raw().get("game_id"));
    // Mirrors Python's assertion: the query protocol has a real Unicode-handling bug for vanilla
    // servers here (the status protocol correctly shows a different string).
    String expectedMotd = String.valueOf((char) 0) + '*' + 'K' + (char) 0xD5;
    assertEquals(expectedMotd, response.motd().toMinecraft());
  }

  @Test
  void queryHandlesUnicodeMotdWith2a00AtTheStart() {
    connection.receive(
        hex(
            "00000000000000000000000000000000686f73746e616d6500006f746865720067616d657479706500534d500067616d655f6964004d"
                + "494e4543524146540076657273696f6e00312e31382e3100706c7567696e7300006d617000776f726c64006e756d706c617965727300"
                + "30006d6178706c617965727300323000686f7374706f727400323535363500686f73746970003137322e31372e302e32000001706c61"
                + "7965725f000000"));

    QueryResponse response = queryClient.readQuery();
    connection.flush();

    assertEquals("MINECRAFT", response.raw().get("game_id"));
    String expectedMotd = String.valueOf((char) 0) + "other";
    assertEquals(expectedMotd, response.motd().toMinecraft());
  }

  @Test
  void sessionIdIsMasked() {
    connection.receive(hex("090000000035373033353037373800"));
    queryClient.handshake();

    byte[] sent = connection.flush();
    assertArrayStartsWith(hex("FEFD09"), sent);
    assertEquals(570350778, queryClient.challenge());
    // Every byte of the generated session id must have its top 4 bits cleared (only the low 4
    // bits per byte are honored, mirroring `& 0x0F0F0F0F`).
    for (int i = 3; i < 7; i++) {
      assertTrue((sent[i] & 0xF0) == 0);
    }
  }
}
