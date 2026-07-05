package de.brentspine.mcstatus4j.protocol.io;

/**
 * Two's complement conversion helpers used by the VarInt/VarLong codec.
 *
 * <p>Python mcstatus needs explicit two's complement math here because Python's {@code int} is
 * arbitrary-precision and has no fixed-width bit pattern of its own. A Java {@code long} is already
 * stored as a 64-bit two's complement value natively, so for {@code bits == 64} these conversions
 * are the identity function: the bit pattern callers want <em>is</em> the bit pattern already held
 * by the {@code long}. For narrower widths ({@code bits < 64}) we still need to fold a negative
 * Java value into its unsigned equivalent within that narrower width (and back), which is what the
 * range-checked branch below does.
 *
 * <p>Because a {@code long} cannot represent values outside {@code [-2^63, 2^63 - 1]}, the "out of
 * range for 64-bit two's complement" failure Python can hit (its ints being unbounded) is not
 * reachable here for {@code bits == 64} — the type system already prevents constructing such a
 * value. This is a disclosed, intentional gap from the Python test suite's 64-bit
 * boundary-rejection cases, not an oversight.
 */
public final class ByteIoUtils {

  private ByteIoUtils() {}

  /**
   * Convert a signed {@code number} into its two's complement bit pattern within the given number
   * of {@code bits}, returned as an unsigned magnitude in a {@code long}.
   *
   * @throws IllegalArgumentException if {@code number} doesn't fit in {@code bits}-bit two's
   *     complement (not checked when {@code bits == 64}, see class docs).
   */
  public static long toTwosComplement(long number, int bits) {
    if (bits == 64) {
      return number;
    }
    long valueMax = 1L << (bits - 1);
    long valueMin = -valueMax;
    if (number >= valueMax || number < valueMin) {
      throw new IllegalArgumentException(
          "Can't convert number "
              + number
              + " into "
              + bits
              + "-bit twos complement format - out of range");
    }
    return number < 0 ? number + (1L << bits) : number;
  }

  /**
   * Convert a {@code bits}-bit unsigned two's complement bit pattern back into its signed value.
   *
   * @throws IllegalArgumentException if {@code number} doesn't fit in {@code bits}-bit two's
   *     complement (not checked when {@code bits == 64}, see class docs).
   */
  public static long fromTwosComplement(long number, int bits) {
    if (bits == 64) {
      return number;
    }
    long valueMax = (1L << bits) - 1;
    if (number < 0 || number > valueMax) {
      throw new IllegalArgumentException(
          "Can't convert number "
              + number
              + " from "
              + bits
              + "-bit twos complement format - out of range");
    }
    if ((number & (1L << (bits - 1))) != 0) {
      number -= 1L << bits;
    }
    return number;
  }
}
