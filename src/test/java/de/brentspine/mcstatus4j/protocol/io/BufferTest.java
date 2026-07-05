package de.brentspine.mcstatus4j.protocol.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Ported from Python mcstatus's {@code tests/protocol/test_connection.py::TestBuffer}. */
class BufferTest {

  private static final HexFormat HEX = HexFormat.of();

  private Buffer buffer;

  @BeforeEach
  void setUp() {
    buffer = new Buffer();
  }

  private static byte[] hex(String hexString) {
    return HEX.parseHex(hexString);
  }

  @Test
  void flush() {
    buffer.write(hex("7FAABB"));
    assertArrayEquals(hex("7FAABB"), buffer.flush());
    assertEquals(0, buffer.getRemaining());
  }

  @Test
  void remaining() {
    buffer.write(hex("7FAABB"));
    assertEquals(3, buffer.getRemaining());
  }

  @Test
  void reset() {
    buffer.write("abcdef".getBytes());
    assertArrayEquals("abc".getBytes(), buffer.read(3));
    buffer.reset();
    assertArrayEquals("abcdef".getBytes(), buffer.read(6));
  }

  @Test
  void clearOnlyAlreadyRead() {
    buffer.write("abcdef".getBytes());
    assertArrayEquals("ab".getBytes(), buffer.read(2));

    buffer.clear(true);

    assertEquals(4, buffer.getRemaining());
    assertArrayEquals("cdef".getBytes(), buffer.read(4));
  }

  @Test
  void unreadView() {
    buffer.write("abcdef".getBytes());
    assertArrayEquals("ab".getBytes(), buffer.read(2));

    byte[] unread = new byte[buffer.unreadView().remaining()];
    buffer.unreadView().get(unread);
    assertArrayEquals("cdef".getBytes(), unread);
  }

  @Test
  void flushOnlyReturnsUnreadData() {
    buffer.write("abcdef".getBytes());
    assertArrayEquals("ab".getBytes(), buffer.read(2));

    assertArrayEquals("cdef".getBytes(), buffer.flush());
    assertEquals(0, buffer.getRemaining());
  }

  @Test
  void multipleWritesAccumulate() {
    buffer.write(hex("7F"));
    buffer.write(hex("AABB"));
    assertArrayEquals(hex("7FAABB"), buffer.flush());
  }

  @Test
  void read() {
    buffer.write(hex("7FAABB"));
    assertArrayEquals(hex("7FAA"), buffer.read(2));
    assertArrayEquals(hex("BB"), buffer.read(1));
  }

  @ParameterizedTest
  @CsvSource({
    "00,0",
    "01,1",
    "0F,15",
    "FFFFFFFF07,2147483647",
    "FFFFFFFF0F,-1",
    "8080808008,-2147483648"
  })
  void varintReadWriteCases(String hexString, long value) {
    int intValue = (int) value;
    buffer.write(hex(hexString));
    assertEquals(intValue, buffer.readVarint());

    buffer.writeVarint(intValue);
    assertArrayEquals(hex(hexString), buffer.flush());
  }

  @Test
  void readInvalidVarint() {
    buffer.write(hex("FFFFFFFF10"));
    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.readVarint());
    assertEquals("Received varint was outside the range of 32-bit int.", ex.getMessage());
  }

  // Python's test_write_invalid_varint passes 2147483648/-2147483649 (out of 32-bit range) to
  // write_varint and expects a ValueError. writeVarint(int) can't even be called with those values
  // in Java - the type system already rejects them at compile time. The underlying range-check
  // logic those values would have exercised is covered directly in
  // ByteIoUtilsTest#toTwosComplementRejectsOutOfRange.

  @Test
  void readUtf() {
    buffer.write(hex("0D48656C6C6F2C20776F726C6421"));
    assertEquals("Hello, world!", buffer.readUtf());
  }

  @Test
  void writeUtf() {
    buffer.writeUtf("Hello, world!");
    assertArrayEquals(hex("0D48656C6C6F2C20776F726C6421"), buffer.flush());
  }

  @Test
  void writeEmptyUtf() {
    buffer.writeUtf("");
    assertArrayEquals(hex("00"), buffer.flush());
  }

  @Test
  void readAscii() {
    buffer.write(hex("48656C6C6F2C20776F726C642100"));
    assertEquals("Hello, world!", buffer.readAscii());
  }

  @Test
  void writeAscii() {
    buffer.writeAscii("Hello, world!");
    assertArrayEquals(hex("48656C6C6F2C20776F726C642100"), buffer.flush());
  }

  @Test
  void writeEmptyAscii() {
    buffer.writeAscii("");
    assertArrayEquals(hex("00"), buffer.flush());
  }

  @Test
  void readShortNegative() {
    buffer.write(hex("8000"));
    assertEquals((short) -32768, buffer.<Short>readValue(StructFormat.SHORT));
  }

  @Test
  void writeShortNegative() {
    buffer.writeValue(StructFormat.SHORT, (short) -32768);
    assertArrayEquals(hex("8000"), buffer.flush());
  }

  @Test
  void readShortPositive() {
    buffer.write(hex("7FFF"));
    assertEquals((short) 32767, buffer.<Short>readValue(StructFormat.SHORT));
  }

  @Test
  void writeShortPositive() {
    buffer.writeValue(StructFormat.SHORT, (short) 32767);
    assertArrayEquals(hex("7FFF"), buffer.flush());
  }

  @Test
  void readUshortPositive() {
    buffer.write(hex("8000"));
    assertEquals(32768, buffer.<Integer>readValue(StructFormat.USHORT));
  }

  @Test
  void writeUshortPositive() {
    buffer.writeValue(StructFormat.USHORT, 32768);
    assertArrayEquals(hex("8000"), buffer.flush());
  }

  @Test
  void readIntNegative() {
    buffer.write(hex("80000000"));
    assertEquals(-2147483648, buffer.<Integer>readValue(StructFormat.INT));
  }

  @Test
  void writeIntNegative() {
    buffer.writeValue(StructFormat.INT, -2147483648);
    assertArrayEquals(hex("80000000"), buffer.flush());
  }

  @Test
  void readIntPositive() {
    buffer.write(hex("7FFFFFFF"));
    assertEquals(2147483647, buffer.<Integer>readValue(StructFormat.INT));
  }

  @Test
  void writeIntPositive() {
    buffer.writeValue(StructFormat.INT, 2147483647);
    assertArrayEquals(hex("7FFFFFFF"), buffer.flush());
  }

  @Test
  void readUintPositive() {
    buffer.write(hex("80000000"));
    assertEquals(2147483648L, buffer.<Long>readValue(StructFormat.UINT));
  }

  @Test
  void writeUintPositive() {
    buffer.writeValue(StructFormat.UINT, 2147483648L);
    assertArrayEquals(hex("80000000"), buffer.flush());
  }

  @Test
  void readLongNegative() {
    buffer.write(hex("8000000000000000"));
    assertEquals(-9223372036854775808L, buffer.<Long>readValue(StructFormat.LONGLONG));
  }

  @Test
  void writeLongNegative() {
    buffer.writeValue(StructFormat.LONGLONG, -9223372036854775808L);
    assertArrayEquals(hex("8000000000000000"), buffer.flush());
  }

  @Test
  void readLongPositive() {
    buffer.write(hex("7FFFFFFFFFFFFFFF"));
    assertEquals(9223372036854775807L, buffer.<Long>readValue(StructFormat.LONGLONG));
  }

  @Test
  void writeLongPositive() {
    buffer.writeValue(StructFormat.LONGLONG, 9223372036854775807L);
    assertArrayEquals(hex("7FFFFFFFFFFFFFFF"), buffer.flush());
  }

  @Test
  void readUlongPositive() {
    buffer.write(hex("8000000000000000"));
    assertEquals(
        new BigInteger("9223372036854775808"),
        buffer.<BigInteger>readValue(StructFormat.ULONGLONG));
  }

  @Test
  void writeUlongPositive() {
    buffer.writeValue(StructFormat.ULONGLONG, new BigInteger("9223372036854775808"));
    assertArrayEquals(hex("8000000000000000"), buffer.flush());
  }

  @ParameterizedTest
  @CsvSource({"01,true", "00,false"})
  void readBool(String hexString, boolean expected) {
    buffer.write(hex(hexString));
    assertEquals(expected, buffer.readBool());
  }

  @ParameterizedTest
  @CsvSource({"01,true", "00,false"})
  void writeBool(String hexString, boolean value) {
    buffer.writeBool(value);
    assertArrayEquals(hex(hexString), buffer.flush());
  }

  @Test
  void readBytearray() {
    buffer.write(hex("027FAA"));
    assertArrayEquals(hex("7FAA"), buffer.readByteArray());
  }

  @Test
  void writeBytearray() {
    buffer.writeByteArray(hex("7FAA"));
    assertArrayEquals(hex("027FAA"), buffer.flush());
  }

  @Test
  void readEmpty() {
    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.read(1));
    assertEquals(
        "Requested to read more data than available. Read 0 bytes: [], out of 1 requested bytes.",
        ex.getMessage());
  }

  @Test
  void readNotEnough() {
    buffer.write("a".getBytes());
    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.read(2));
    assertEquals(
        "Requested to read more data than available. Read 1 bytes: [97], out of 2 requested bytes.",
        ex.getMessage());
  }

  @Test
  void readNegativeLength() {
    ProtocolReadException ex = assertThrows(ProtocolReadException.class, () -> buffer.read(-1));
    assertEquals("Requested to read a negative amount of data: -1.", ex.getMessage());
  }
}
