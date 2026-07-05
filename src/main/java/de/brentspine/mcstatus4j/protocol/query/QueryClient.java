package de.brentspine.mcstatus4j.protocol.query;

import de.brentspine.mcstatus4j.protocol.io.Buffer;
import de.brentspine.mcstatus4j.protocol.io.Connection;
import de.brentspine.mcstatus4j.protocol.io.UdpConnection;
import de.brentspine.mcstatus4j.responses.QueryResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The GameSpy4 Query protocol client (UDP), used by {@code JavaServer.query()}.
 *
 * <p>Mirrors Python mcstatus's {@code _BaseQueryClient}/{@code QueryClient}, collapsed into a
 * single blocking class - see {@link Connection}'s class docs for why.
 */
public final class QueryClient {

  private static final byte[] MAGIC_PREFIX = {(byte) 0xFE, (byte) 0xFD};
  private static final byte[] PADDING = {0, 0, 0, 0};
  private static final int PACKET_TYPE_CHALLENGE = 9;
  private static final int PACKET_TYPE_QUERY = 0;

  /**
   * Looks ahead from the start of the (motd-as-)"hostname" value to the next known key name, since
   * the query protocol's motd field isn't reliably NUL-safe/Unicode-safe on its own.
   */
  private static final Pattern MOTD_LOOKAHEAD =
      Pattern.compile(
          "(.*?)"
              + (char) 0
              + "(hostip|hostport|game_id|gametype|map|maxplayers|numplayers|plugins|version)",
          Pattern.DOTALL);

  private final Connection connection;
  private int challenge;

  public QueryClient(Connection connection) {
    this.connection = connection;
  }

  public int challenge() {
    return challenge;
  }

  private static int generateSessionId() {
    // Mirrors Python's random.randint(0, 2**31) & 0x0F0F0F0F: Minecraft only honors the low 4 bits
    // of each byte, so any reasonably uniform 32-bit source works once masked.
    return ThreadLocalRandom.current().nextInt() & 0x0F0F0F0F;
  }

  private Buffer createPacket() {
    Buffer packet = new Buffer();
    packet.write(MAGIC_PREFIX);
    packet.writeUnsignedByte(PACKET_TYPE_QUERY);
    packet.writeUnsignedInt(Integer.toUnsignedLong(generateSessionId()));
    packet.writeInt(challenge);
    packet.write(PADDING);
    return packet;
  }

  private Buffer createHandshakePacket() {
    Buffer packet = new Buffer();
    packet.write(MAGIC_PREFIX);
    packet.writeUnsignedByte(PACKET_TYPE_CHALLENGE);
    packet.writeUnsignedInt(Integer.toUnsignedLong(generateSessionId()));
    return packet;
  }

  private Buffer readPacket() {
    Buffer packet = new Buffer(connection.read(UdpConnection.MAX_DATAGRAM_SIZE));
    packet.read(1 + 4); // Strip the 1-byte packet type + 4-byte session id response prefix.
    return packet;
  }

  /** Perform the challenge handshake, populating {@link #challenge()} for subsequent queries. */
  public void handshake() {
    connection.write(createHandshakePacket().flush());
    Buffer packet = readPacket();
    challenge = Integer.parseInt(packet.readAscii());
  }

  /** Send a full-stat query request and read/parse the response. */
  public QueryResponse readQuery() {
    connection.write(createPacket().flush());
    Buffer response = readPacket();
    ParsedResponse parsed = parseResponse(response);
    return QueryResponse.build(parsed.raw(), parsed.players());
  }

  private record ParsedResponse(Map<String, Object> raw, List<String> players) {}

  private static ParsedResponse parseResponse(Buffer response) {
    response.read("splitnum".length() + 3);
    Map<String, Object> data = new LinkedHashMap<>();

    while (true) {
      String key = response.readAscii();
      if ("hostname".equals(key)) {
        String unread = new String(toByteArray(response.unreadView()), StandardCharsets.ISO_8859_1);
        Matcher matcher = MOTD_LOOKAHEAD.matcher(unread);
        String motd = matcher.find() ? matcher.group(1) : "";
        byte[] motdBytes = response.read(motd.length());
        data.put(key, new String(motdBytes, StandardCharsets.ISO_8859_1));
        response.read(1); // Ignore the NUL byte.
      } else if (key.isEmpty()) {
        response.read(1);
        break;
      } else {
        data.put(key, response.readAscii());
      }
    }

    response.read("player_".length() + 2);

    List<String> playersList = new ArrayList<>();
    while (true) {
      String player = response.readAscii();
      if (player.isEmpty()) {
        break;
      }
      playersList.add(player);
    }

    return new ParsedResponse(data, playersList);
  }

  private static byte[] toByteArray(ByteBuffer buffer) {
    byte[] array = new byte[buffer.remaining()];
    buffer.get(array);
    return array;
  }
}
