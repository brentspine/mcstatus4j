package de.brentspine.mcstatus4j.responses;

import de.brentspine.mcstatus4j.motd.Motd;
import java.util.Map;

/**
 * Shared shape of a status response (Java, Bedrock, or Legacy edition). Note {@link QueryResponse}
 * is deliberately not part of this hierarchy - it has a different shape (no single {@code
 * players}/{@code version} pair in the same sense), matching Python mcstatus's own {@code
 * QueryResponse} standing apart from {@code BaseStatusResponse}.
 */
public sealed interface BaseStatusResponse
    permits JavaStatusResponse, BedrockStatusResponse, LegacyStatusResponse {

  BaseStatusPlayers players();

  BaseStatusVersion version();

  Motd motd();

  /** Latency between the server and the client, in milliseconds. */
  double latency();

  /** Alias for {@code motd().toMinecraft()}. */
  default String description() {
    return motd().toMinecraft();
  }

  /**
   * A JSON-serializable {@link Map} view of this response, with {@code motd} rendered as a
   * simplified Minecraft {@code §}-code string rather than the full parsed component tree (mirrors
   * Python mcstatus's {@code as_dict()}).
   */
  Map<String, Object> asDict();
}
