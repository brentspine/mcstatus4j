package de.brentspine.mcstatus4j.protocol.bedrock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import de.brentspine.mcstatus4j.responses.BedrockStatusResponse;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Ported from Python mcstatus's {@code tests/protocol/test_bedrock_client.py}. */
class BedrockClientTest {

  // Exact bytes from Python's test fixture, computed via `data.hex()` to guarantee byte-for-byte
  // fidelity rather than hand-transcribing the embedded UTF-8 section-sign sequences.
  private static final String GOLDEN_PONG_HEX =
      "1c000000000000000034475400b88344de00ffff00fefefefefdfdfdfd1234567800774d4350453bc2a772c2a73447c2a772"
          + "c2a73661c2a772c2a76579c2a772c2a73242c2a772c2a7316fc2a772c2a73977c2a772c2a76473c2a772c2a73465c2a772"
          + "c2a736723b3432323b3b313b36393b333736373037313937353339313035333032323b3b44656661756c743b313b3139"
          + "3133323b2d313b";

  @Test
  void parseResponseReturnsExpectedType() {
    byte[] data = HexFormat.of().parseHex(GOLDEN_PONG_HEX);
    BedrockStatusResponse parsed = BedrockClient.parseResponse(data, 1);
    assertInstanceOf(BedrockStatusResponse.class, parsed);
  }

  @Test
  void parseResponseFieldsAreCorrect() {
    byte[] data = HexFormat.of().parseHex(GOLDEN_PONG_HEX);
    BedrockStatusResponse parsed = BedrockClient.parseResponse(data, 1);

    assertEquals("MCPE", parsed.version().brand());
    assertEquals(422, parsed.version().protocol());
    assertEquals("", parsed.version().name());
    assertEquals(1, parsed.players().online());
    assertEquals(69, parsed.players().max());
    assertEquals("", parsed.mapName());
    assertEquals("Default", parsed.gamemode());
  }
}
