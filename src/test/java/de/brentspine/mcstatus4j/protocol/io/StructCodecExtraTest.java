package de.brentspine.mcstatus4j.protocol.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link StructFormat} members not exercised by any real mcstatus4j protocol client
 * (FLOAT/DOUBLE/HALFFLOAT/LONG/ULONG/ULONGLONG/CHAR) but kept for parity with Python mcstatus's
 * public {@code StructFormat} surface. See {@link StructFormat}'s class docs.
 */
class StructCodecExtraTest {

  private Buffer buffer;

  @BeforeEach
  void setUp() {
    buffer = new Buffer();
  }

  @Test
  void floatRoundtrip() {
    buffer.writeValue(StructFormat.FLOAT, 1.5f);
    assertEquals(1.5f, buffer.<Float>readValue(StructFormat.FLOAT));
  }

  @Test
  void doubleRoundtrip() {
    buffer.writeValue(StructFormat.DOUBLE, 3.14159265358979);
    assertEquals(3.14159265358979, buffer.<Double>readValue(StructFormat.DOUBLE));
  }

  @Test
  void longFormatIs4ByteSigned() {
    buffer.writeValue(StructFormat.LONG, -2147483648);
    assertEquals(-2147483648, buffer.<Integer>readValue(StructFormat.LONG));
  }

  @Test
  void ulongFormatIs4ByteUnsigned() {
    buffer.writeValue(StructFormat.ULONG, 2147483648L);
    assertEquals(2147483648L, buffer.<Long>readValue(StructFormat.ULONG));
  }

  @Test
  void ulonglongRejectsNegativeBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> buffer.writeValue(StructFormat.ULONGLONG, BigInteger.valueOf(-1)));
  }

  @Test
  void ulonglongRejectsOverflowingBigInteger() {
    assertThrows(
        IllegalArgumentException.class,
        () -> buffer.writeValue(StructFormat.ULONGLONG, BigInteger.ONE.shiftLeft(64)));
  }

  @Test
  void ulonglongAcceptsPlainLong() {
    buffer.writeValue(StructFormat.ULONGLONG, 42L);
    assertEquals(BigInteger.valueOf(42), buffer.<BigInteger>readValue(StructFormat.ULONGLONG));
  }

  @Test
  void halfFloatZeroRoundtrips() {
    buffer.writeValue(StructFormat.HALFFLOAT, 0.0f);
    assertEquals(0.0f, buffer.<Float>readValue(StructFormat.HALFFLOAT));
  }

  @Test
  void halfFloatNegativeRoundtrips() {
    buffer.writeValue(StructFormat.HALFFLOAT, -2.5f);
    assertEquals(-2.5f, buffer.<Float>readValue(StructFormat.HALFFLOAT), 0.01f);
  }

  @Test
  void halfFloatNormalValueRoundtrips() {
    buffer.writeValue(StructFormat.HALFFLOAT, 1.0f);
    assertEquals(1.0f, buffer.<Float>readValue(StructFormat.HALFFLOAT), 0.001f);
  }

  @Test
  void halfFloatUnderflowsToZero() {
    // Far too small to represent as a half-precision float (and not handled as a subnormal by
    // this codec) - should underflow to zero rather than throwing.
    buffer.writeValue(StructFormat.HALFFLOAT, 1.0e-10f);
    assertEquals(0.0f, buffer.<Float>readValue(StructFormat.HALFFLOAT));
  }

  @Test
  void halfFloatOverflowsToInfinity() {
    buffer.writeValue(StructFormat.HALFFLOAT, 1.0e10f);
    assertEquals(Float.POSITIVE_INFINITY, buffer.<Float>readValue(StructFormat.HALFFLOAT));
  }

  @Test
  void halfFloatDecodesSubnormal() {
    // Smallest positive half-precision subnormal: sign=0, exponent=0, mantissa=1 -> 2^-24.
    buffer.write(new byte[] {0x00, 0x01});
    float decoded = buffer.<Float>readValue(StructFormat.HALFFLOAT);
    assertEquals(Math.pow(2, -24), decoded, 1e-10);
  }

  @Test
  void halfFloatDecodesInfinityAndNan() {
    buffer.write(new byte[] {0x7C, 0x00}); // +Infinity
    assertEquals(Float.POSITIVE_INFINITY, buffer.<Float>readValue(StructFormat.HALFFLOAT));

    buffer.write(new byte[] {0x7C, 0x01}); // NaN (non-zero mantissa)
    assertEquals(Float.NaN, buffer.<Float>readValue(StructFormat.HALFFLOAT));
  }
}
