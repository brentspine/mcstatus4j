package de.brentspine.mcstatus4j.protocol.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Ported from Python mcstatus's {@code tests/protocol/test_base_io_twos_complement.py}.
 *
 * <p>The 64-bit out-of-range rejection cases from the Python suite are intentionally not ported:
 * they rely on Python's unbounded {@code int} representing values outside {@code [-2^63, 2^63 -
 * 1]}, which a Java {@code long} parameter cannot even hold in the first place. See {@link
 * ByteIoUtils}'s class docs.
 */
class ByteIoUtilsTest {

  private static Stream<Arguments> twosComplementCases() {
    return Stream.of(
        Arguments.of(-128L, 8, 0x80L),
        Arguments.of(-2L, 8, 0xFEL),
        Arguments.of(-1L, 8, 0xFFL),
        Arguments.of(0L, 8, 0x00L),
        Arguments.of(1L, 8, 0x01L),
        Arguments.of(127L, 8, 0x7FL),
        Arguments.of(-(1L << 15), 16, 0x8000L),
        Arguments.of(-1L, 16, 0xFFFFL),
        Arguments.of((1L << 15) - 1, 16, 0x7FFFL),
        Arguments.of(-(1L << 31), 32, 0x80000000L),
        Arguments.of(-1L, 32, 0xFFFFFFFFL),
        Arguments.of((1L << 31) - 1, 32, 0x7FFFFFFFL),
        Arguments.of(Long.MIN_VALUE, 64, 0x8000000000000000L),
        Arguments.of(-9_876_543_210_123_456L, 64, 0xFFDCE956165A0F40L),
        Arguments.of(-1L, 64, 0xFFFFFFFFFFFFFFFFL),
        Arguments.of(0L, 64, 0x0000000000000000L),
        Arguments.of(9_876_543_210_123_456L, 64, 0x002316A9E9A5F0C0L),
        Arguments.of(Long.MAX_VALUE, 64, 0x7FFFFFFFFFFFFFFFL));
  }

  @ParameterizedTest
  @MethodSource("twosComplementCases")
  void toTwosComplementMatchesExpectedValues(long number, int bits, long expectedTwos) {
    assertEquals(expectedTwos, ByteIoUtils.toTwosComplement(number, bits));
  }

  @ParameterizedTest
  @MethodSource("twosComplementCases")
  void fromTwosComplementMatchesExpectedValues(long number, int bits, long twosValue) {
    assertEquals(number, ByteIoUtils.fromTwosComplement(twosValue, bits));
  }

  private static Stream<Arguments> toTwosComplementOutOfRange() {
    return Stream.of(
        Arguments.of(-129L, 8),
        Arguments.of(128L, 8),
        Arguments.of(-(1L << 31) - 1, 32),
        Arguments.of(1L << 31, 32));
  }

  @ParameterizedTest
  @MethodSource("toTwosComplementOutOfRange")
  void toTwosComplementRejectsOutOfRange(long number, int bits) {
    assertThrows(IllegalArgumentException.class, () -> ByteIoUtils.toTwosComplement(number, bits));
  }

  private static Stream<Arguments> fromTwosComplementOutOfRange() {
    return Stream.of(Arguments.of(-1L, 8), Arguments.of(256L, 8), Arguments.of(1L << 32, 32));
  }

  @ParameterizedTest
  @MethodSource("fromTwosComplementOutOfRange")
  void fromTwosComplementRejectsOutOfRange(long number, int bits) {
    assertThrows(
        IllegalArgumentException.class, () -> ByteIoUtils.fromTwosComplement(number, bits));
  }
}
