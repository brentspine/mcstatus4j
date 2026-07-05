package de.brentspine.mcstatus4j.protocol.io;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Encodes/decodes the fixed-width {@link StructFormat} values to/from big-endian byte arrays.
 *
 * <p>Package-private: {@link ProtocolReader#readValue} and {@link ProtocolWriter#writeValue} are
 * the public entry points. Mirrors Python mcstatus's use of {@code struct.pack}/{@code
 * struct.unpack} with a {@code ">"} (big-endian, standard-size) prefix.
 */
final class StructCodec {

  private static final long UINT_MAX = 0xFFFFFFFFL;

  private StructCodec() {}

  static byte[] encode(StructFormat fmt, Object value) {
    ByteBuffer buffer = ByteBuffer.allocate(fmt.byteWidth());
    switch (fmt) {
      case BOOL -> buffer.put((byte) (((Boolean) value) ? 1 : 0));
      case CHAR -> buffer.put((Byte) value);
      case BYTE ->
          buffer.put(requireInRange(((Number) value).longValue(), -128, 127, fmt).byteValue());
      case UBYTE ->
          buffer.put(requireInRange(((Number) value).longValue(), 0, 255, fmt).byteValue());
      case SHORT ->
          buffer.putShort(
              requireInRange(((Number) value).longValue(), -32_768, 32_767, fmt).shortValue());
      case USHORT ->
          buffer.putShort(
              requireInRange(((Number) value).longValue(), 0, 65_535, fmt).shortValue());
      case INT, LONG ->
          buffer.putInt(
              requireInRange(
                      ((Number) value).longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE, fmt)
                  .intValue());
      case UINT, ULONG ->
          buffer.putInt(
              (int) requireInRange(((Number) value).longValue(), 0, UINT_MAX, fmt).longValue());
      case FLOAT -> buffer.putFloat(((Number) value).floatValue());
      case DOUBLE -> buffer.putDouble(((Number) value).doubleValue());
      case HALFFLOAT -> buffer.putShort(HalfFloat.floatToHalf(((Number) value).floatValue()));
      case LONGLONG -> buffer.putLong(((Number) value).longValue());
      case ULONGLONG -> buffer.putLong(toUnsignedLongBits(value));
    }
    return buffer.array();
  }

  @SuppressWarnings("unchecked")
  static <T> T decode(StructFormat fmt, byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    Object result =
        switch (fmt) {
          case BOOL -> buffer.get() != 0;
          case CHAR -> buffer.get();
          case BYTE -> buffer.get();
          case UBYTE -> buffer.get() & 0xFF;
          case SHORT -> buffer.getShort();
          case USHORT -> buffer.getShort() & 0xFFFF;
          case INT, LONG -> buffer.getInt();
          case UINT, ULONG -> Integer.toUnsignedLong(buffer.getInt());
          case FLOAT -> buffer.getFloat();
          case DOUBLE -> buffer.getDouble();
          case HALFFLOAT -> HalfFloat.halfToFloat(buffer.getShort());
          case LONGLONG -> buffer.getLong();
          case ULONGLONG -> unsignedLongBitsToBigInteger(buffer.getLong());
        };
    return (T) result;
  }

  private static Long requireInRange(long value, long min, long max, StructFormat fmt) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
          "Value "
              + value
              + " is out of range for "
              + fmt
              + " (expected ["
              + min
              + ", "
              + max
              + "]).");
    }
    return value;
  }

  private static long toUnsignedLongBits(Object value) {
    if (value instanceof BigInteger bigInteger) {
      if (bigInteger.signum() < 0 || bigInteger.bitLength() > 64) {
        throw new IllegalArgumentException(
            "Value " + bigInteger + " is out of range for ULONGLONG.");
      }
      return bigInteger.longValue();
    }
    return ((Number) value).longValue();
  }

  private static BigInteger unsignedLongBitsToBigInteger(long bits) {
    if (bits >= 0) {
      return BigInteger.valueOf(bits);
    }
    return BigInteger.valueOf(bits).add(BigInteger.ONE.shiftLeft(64));
  }

  /** IEEE 754 binary16 (half precision) &lt;-&gt; Java {@code float} conversion. */
  private static final class HalfFloat {

    private HalfFloat() {}

    static short floatToHalf(float value) {
      int bits = Float.floatToIntBits(value);
      int sign = (bits >>> 16) & 0x8000;
      int exponent = ((bits >>> 23) & 0xFF) - 127 + 15;
      int mantissa = bits & 0x7FFFFF;

      if (exponent <= 0) {
        return (short) sign; // Underflows to zero (subnormals not supported).
      }
      if (exponent >= 0x1F) {
        return (short) (sign | 0x7C00); // Overflows to infinity.
      }
      return (short) (sign | (exponent << 10) | (mantissa >>> 13));
    }

    static float halfToFloat(short half) {
      int bits = half & 0xFFFF;
      int sign = (bits & 0x8000) << 16;
      int exponent = (bits >>> 10) & 0x1F;
      int mantissa = bits & 0x3FF;

      if (exponent == 0) {
        if (mantissa == 0) {
          return Float.intBitsToFloat(sign);
        }
        // Subnormal half -> normalized float.
        exponent = 1;
        while ((mantissa & 0x400) == 0) {
          mantissa <<= 1;
          exponent--;
        }
        mantissa &= 0x3FF;
      } else if (exponent == 0x1F) {
        return Float.intBitsToFloat(sign | 0x7F800000 | (mantissa << 13));
      }

      int floatExponent = exponent - 15 + 127;
      int floatBits = sign | (floatExponent << 23) | (mantissa << 13);
      return Float.intBitsToFloat(floatBits);
    }
  }
}
