package de.brentspine.mcstatus4j.protocol.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Ported from Python mcstatus's {@code tests/protocol/test_base_io.py}. Python parametrizes every
 * case over both a sync and an async fake connection; mcstatus4j has no such duality (see {@link
 * ProtocolReader}'s class docs), so these run against a single {@link Buffer} instance.
 */
class ProtocolIoTest {

  private static final HexFormat HEX = HexFormat.of();

  private Buffer buffer;

  @BeforeEach
  void setUp() {
    buffer = new Buffer();
  }

  private static byte[] hex(String hexString) {
    return HEX.parseHex(hexString);
  }

  private static Stream<Arguments> byteValues() {
    return Stream.of(
        Arguments.of(StructFormat.UBYTE, 0, "00"),
        Arguments.of(StructFormat.UBYTE, 15, "0f"),
        Arguments.of(StructFormat.UBYTE, 255, "ff"),
        Arguments.of(StructFormat.BYTE, (byte) 0, "00"),
        Arguments.of(StructFormat.BYTE, (byte) 15, "0f"),
        Arguments.of(StructFormat.BYTE, (byte) 127, "7f"),
        Arguments.of(StructFormat.BYTE, (byte) -20, "ec"),
        Arguments.of(StructFormat.BYTE, (byte) -128, "80"));
  }

  @ParameterizedTest
  @MethodSource("byteValues")
  void writeValueMatchesReference(StructFormat fmt, Object value, String expectedHex) {
    buffer.writeValue(fmt, value);
    assertArrayEquals(hex(expectedHex), buffer.flush());
  }

  @ParameterizedTest
  @MethodSource("byteValues")
  void readValueMatchesReference(StructFormat fmt, Object expected, String encodedHex) {
    buffer.write(hex(encodedHex));
    assertEquals(expected, buffer.readValue(fmt));
  }

  @Test
  void writeValueCharUsesSingleByte() {
    buffer.writeValue(StructFormat.CHAR, (byte) 'a');
    assertArrayEquals("a".getBytes(StandardCharsets.US_ASCII), buffer.flush());
  }

  @Test
  void readValueCharReturnsByte() {
    buffer.write("a".getBytes(StandardCharsets.US_ASCII));
    assertEquals((byte) 'a', buffer.<Byte>readValue(StructFormat.CHAR));
  }

  private static Stream<Arguments> outOfRangeWrites() {
    return Stream.of(
        Arguments.of(StructFormat.UBYTE, -1), Arguments.of(StructFormat.UBYTE, 256),
        Arguments.of(StructFormat.BYTE, -129), Arguments.of(StructFormat.BYTE, 128));
  }

  @ParameterizedTest
  @MethodSource("outOfRangeWrites")
  void writeValueRejectsOutOfRange(StructFormat fmt, int value) {
    assertThrows(IllegalArgumentException.class, () -> buffer.writeValue(fmt, value));
  }

  private static Stream<Arguments> varuintCases() {
    return Stream.of(
        Arguments.of(0L, "00"),
        Arguments.of(127L, "7f"),
        Arguments.of(128L, "8001"),
        Arguments.of(255L, "ff01"),
        Arguments.of(1_000_000L, "c0843d"),
        Arguments.of((1L << 31) - 1, "ffffffff07"));
  }

  @ParameterizedTest
  @MethodSource("varuintCases")
  void writeVaruintMatchesReference(long number, String expectedHex) {
    buffer.writeVaruint(number, 32);
    assertArrayEquals(hex(expectedHex), buffer.flush());
  }

  @ParameterizedTest
  @MethodSource("varuintCases")
  void readVaruintMatchesReference(long expected, String encodedHex) {
    buffer.write(hex(encodedHex));
    assertEquals(expected, buffer.readVaruint(32));
  }

  private static Stream<Arguments> writeVaruintOutOfRange() {
    return Stream.of(Arguments.of(-1L, 1), Arguments.of(1L << 16, 16), Arguments.of(1L << 32, 32));
  }

  @ParameterizedTest
  @MethodSource("writeVaruintOutOfRange")
  void writeVaruintRejectsOutOfRange(long number, int maxBits) {
    assertThrows(IllegalArgumentException.class, () -> buffer.writeVaruint(number, maxBits));
  }

  @Test
  void readVaruintRejectsOutOfRange16Bit() {
    buffer.write(hex("808004"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> buffer.readVaruint(16));
    assertEquals("Received varint was outside the range of 16-bit int.", ex.getMessage());
  }

  @Test
  void readVaruintRejectsOutOfRange32Bit() {
    buffer.write(hex("8080808010"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> buffer.readVaruint(32));
    assertEquals("Received varint was outside the range of 32-bit int.", ex.getMessage());
  }

  @Test
  void readVaruintRejectsTooManyBytes16Bit() {
    buffer.write(hex("80808000"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> buffer.readVaruint(16));
    assertEquals(
        "Received varint had too many bytes for 16-bit int (continuation bit set on byte 3).",
        ex.getMessage());
  }

  @Test
  void readVaruintRejectsTooManyBytes32Bit() {
    buffer.write(hex("808080808000"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> buffer.readVaruint(32));
    assertEquals(
        "Received varint had too many bytes for 32-bit int (continuation bit set on byte 5).",
        ex.getMessage());
  }

  private static Stream<Arguments> varintCases() {
    return Stream.of(
        Arguments.of(127, "7f"),
        Arguments.of(16_384, "808001"),
        Arguments.of(-128, "80ffffff0f"),
        Arguments.of(-16_383, "8180ffff0f"));
  }

  @ParameterizedTest
  @MethodSource("varintCases")
  void writeVarintMatchesReference(int number, String expectedHex) {
    buffer.writeVarint(number);
    assertArrayEquals(hex(expectedHex), buffer.flush());
  }

  @ParameterizedTest
  @MethodSource("varintCases")
  void readVarintMatchesReference(int expected, String encodedHex) {
    buffer.write(hex(encodedHex));
    assertEquals(expected, buffer.readVarint());
  }

  private static Stream<Arguments> varlongCases() {
    return Stream.of(
        Arguments.of(127L, "7f"),
        Arguments.of(16_384L, "808001"),
        Arguments.of(-128L, "80ffffffffffffffff01"),
        Arguments.of(-16_383L, "8180ffffffffffffff01"));
  }

  @ParameterizedTest
  @MethodSource("varlongCases")
  void writeVarlongMatchesReference(long number, String expectedHex) {
    buffer.writeVarlong(number);
    assertArrayEquals(hex(expectedHex), buffer.flush());
  }

  @ParameterizedTest
  @MethodSource("varlongCases")
  void readVarlongMatchesReference(long expected, String encodedHex) {
    buffer.write(hex(encodedHex));
    assertEquals(expected, buffer.readVarlong());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 127, 16_384, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void varintRoundtrip(int number) {
    buffer.writeVarint(number);
    buffer.write(buffer.flush());
    assertEquals(number, buffer.readVarint());
  }

  @ParameterizedTest
  @ValueSource(longs = {127, 16_384, -128, -16_383, Long.MIN_VALUE, Long.MAX_VALUE})
  void varlongRoundtrip(long number) {
    buffer.writeVarlong(number);
    buffer.write(buffer.flush());
    assertEquals(number, buffer.readVarlong());
  }

  @Test
  void optionalHelpers() {
    Function<String, String> writer = value -> "written:" + value;

    assertEquals(null, buffer.writeOptional(null, writer));
    assertArrayEquals(hex("00"), buffer.flush());

    assertEquals("written:value", buffer.writeOptional("value", writer));
    assertArrayEquals(hex("01"), buffer.flush());

    buffer.write(hex("00"));
    assertEquals(null, buffer.readOptional(() -> "parsed"));

    buffer.write(hex("01"));
    assertEquals("parsed", buffer.readOptional(() -> "parsed"));
  }

  @Test
  void writeAndReadAscii() {
    buffer.writeAscii("hello");
    buffer.write(buffer.flush());
    assertEquals("hello", buffer.readAscii());
  }

  @Test
  void writeAndReadBytearray() {
    byte[] data = hex("000168656c6c6fff");
    buffer.writeByteArray(data);
    buffer.write(buffer.flush());
    assertArrayEquals(data, buffer.readByteArray());
  }

  @Test
  void readBytearrayRejectsNegativeLength() {
    buffer.write(hex("ffffffff0f"));
    ProtocolReadException ex =
        assertThrows(ProtocolReadException.class, () -> buffer.readByteArray());
    assertEquals("Length prefix for byte arrays must be non-negative, got -1.", ex.getMessage());
  }

  @Test
  void writeUtfRejectsTooManyCharacters() {
    String tooLong = "a".repeat(32_768);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> buffer.writeUtf(tooLong));
    assertEquals(
        "Maximum character limit for writing strings is 32767 characters.", ex.getMessage());
  }

  @Test
  void readUtfRejectsTooManyBytes() {
    Buffer payload = new Buffer();
    payload.writeVarint(131_069);
    buffer.write(payload.flush());

    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.readUtf());
    assertEquals(
        "Maximum read limit for utf strings is 131068 bytes, got 131069.", ex.getMessage());
  }

  @Test
  void readUtfRejectsNegativeLength() {
    buffer.write(hex("ffffffff0f"));
    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.readUtf());
    assertEquals("Length prefix for utf strings must be non-negative, got -1.", ex.getMessage());
  }

  @Test
  void readUtfRejectsTooManyCharacters() {
    String text = "a".repeat(32_768);
    byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
    Buffer payload = new Buffer();
    payload.writeVarint(textBytes.length);
    payload.write(textBytes);
    buffer.write(payload.flush());

    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.readUtf());
    assertEquals(
        "Maximum read limit for utf strings is 32767 characters, got 32768.", ex.getMessage());
  }

  @Test
  void writeAndReadNonAsciiUtf() {
    // Non-ASCII multi-byte UTF-8 text, mirroring Python's "Hindi text" test case.
    String text = "नमस्ते";
    buffer.writeUtf(text);
    buffer.write(buffer.flush());
    assertEquals(text, buffer.readUtf());
  }
}
