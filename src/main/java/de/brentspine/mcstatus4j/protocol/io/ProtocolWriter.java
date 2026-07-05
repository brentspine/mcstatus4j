package de.brentspine.mcstatus4j.protocol.io;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Shared binary write primitives used by every protocol client.
 *
 * <p>Mirrors Python mcstatus's {@code BaseSyncWriter}/{@code BaseAsyncWriter}, collapsed into a
 * single (blocking) interface since mcstatus4j exposes async operations via {@code
 * CompletableFuture} wrapping the blocking implementation rather than a parallel non-blocking
 * protocol stack.
 */
public interface ProtocolWriter {

  /** Underlying write method; every other method here eventually calls this. */
  void write(byte[] data);

  /** Write {@code value} in the big-endian binary layout described by {@code fmt}. */
  default void writeValue(StructFormat fmt, Object value) {
    write(StructCodec.encode(fmt, value));
  }

  default void writeBool(boolean value) {
    writeValue(StructFormat.BOOL, value);
  }

  default void writeByte(byte value) {
    writeValue(StructFormat.BYTE, value);
  }

  default void writeUnsignedByte(int value) {
    writeValue(StructFormat.UBYTE, value);
  }

  default void writeShort(short value) {
    writeValue(StructFormat.SHORT, value);
  }

  default void writeUnsignedShort(int value) {
    writeValue(StructFormat.USHORT, value);
  }

  default void writeInt(int value) {
    writeValue(StructFormat.INT, value);
  }

  default void writeUnsignedInt(long value) {
    writeValue(StructFormat.UINT, value);
  }

  default void writeLongLong(long value) {
    writeValue(StructFormat.LONGLONG, value);
  }

  /**
   * Write an arbitrarily large unsigned integer using a variable-length encoding: 7 data bits per
   * byte, little-endian by 7-bit group, with the MSB of each byte as a continuation flag.
   *
   * @param value the unsigned magnitude to encode, held as the bit pattern of a {@code long} (see
   *     {@link ByteIoUtils} for why this is safe up to 64 bits).
   * @param maxBits maximum allowed bit width for the encoded value. Not validated when {@code
   *     maxBits == 64}, since every 64-bit bit pattern is inherently in range.
   * @throws IllegalArgumentException if {@code value} is negative or exceeds {@code maxBits} (only
   *     checked when {@code maxBits < 64}).
   */
  default void writeVaruint(long value, int maxBits) {
    if (maxBits < 64) {
      long valueMax = (1L << maxBits) - 1;
      if (value < 0 || value > valueMax) {
        throw new IllegalArgumentException(
            "Tried to write varint outside of the range of " + maxBits + "-bit int.");
      }
    }

    long remaining = value;
    while (true) {
      if ((remaining & ~0x7FL) == 0) {
        writeUnsignedByte((int) remaining);
        return;
      }
      writeUnsignedByte((int) ((remaining & 0x7F) | 0x80));
      remaining >>>= 7;
    }
  }

  /** Write a 32-bit signed integer using a variable-length encoding. See {@link #writeVaruint}. */
  default void writeVarint(int value) {
    long unsigned = ByteIoUtils.toTwosComplement(value, 32);
    writeVaruint(unsigned, 32);
  }

  /** Write a 64-bit signed integer using a variable-length encoding. See {@link #writeVaruint}. */
  default void writeVarlong(long value) {
    long unsigned = ByteIoUtils.toTwosComplement(value, 64);
    writeVaruint(unsigned, 64);
  }

  /** Write an arbitrary sequence of bytes, prefixed with a varint of its size. */
  default void writeByteArray(byte[] data) {
    writeVarint(data.length);
    write(data);
  }

  /** Write an ISO-8859-1 encoded string, with a NUL (0x00) terminator. */
  default void writeAscii(String value) {
    write(value.getBytes(StandardCharsets.ISO_8859_1));
    write(new byte[] {0});
  }

  /**
   * Write a UTF-8 encoded string prefixed with its byte length as a varint.
   *
   * @throws IllegalArgumentException if {@code value} exceeds 32767 characters.
   */
  default void writeUtf(String value) {
    if (value.length() > 32_767) {
      throw new IllegalArgumentException(
          "Maximum character limit for writing strings is 32767 characters.");
    }
    byte[] data = value.getBytes(StandardCharsets.UTF_8);
    writeVarint(data.length);
    write(data);
  }

  /**
   * Write a bool indicating whether {@code value} is present and, if so, serialize it via {@code
   * writer}.
   *
   * @return {@code null} if {@code value} is {@code null}, otherwise {@code writer}'s result.
   */
  default <T, R> R writeOptional(T value, Function<T, R> writer) {
    if (value == null) {
      writeBool(false);
      return null;
    }
    writeBool(true);
    return writer.apply(value);
  }
}
