package de.brentspine.mcstatus4j.protocol.io;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Shared binary read primitives used by every protocol client.
 *
 * <p>Mirrors Python mcstatus's {@code BaseSyncReader}/{@code BaseAsyncReader}, collapsed into a
 * single (blocking) interface. See {@link ProtocolWriter} for why.
 */
public interface ProtocolReader {

  /** Underlying read method; every other method here eventually calls this. */
  byte[] read(int length);

  /** Read a value in the big-endian binary layout described by {@code fmt}. */
  default <T> T readValue(StructFormat fmt) {
    return StructCodec.decode(fmt, read(fmt.byteWidth()));
  }

  default boolean readBool() {
    return this.<Boolean>readValue(StructFormat.BOOL);
  }

  default byte readByte() {
    return this.<Byte>readValue(StructFormat.BYTE);
  }

  default int readUnsignedByte() {
    return this.<Integer>readValue(StructFormat.UBYTE);
  }

  default short readShort() {
    return this.<Short>readValue(StructFormat.SHORT);
  }

  default int readUnsignedShort() {
    return this.<Integer>readValue(StructFormat.USHORT);
  }

  default int readInt() {
    return this.<Integer>readValue(StructFormat.INT);
  }

  default long readUnsignedInt() {
    return this.<Long>readValue(StructFormat.UINT);
  }

  default long readLongLong() {
    return this.<Long>readValue(StructFormat.LONGLONG);
  }

  /**
   * Read an arbitrarily large unsigned integer using a variable-length encoding. See {@link
   * ProtocolWriter#writeVaruint} for the encoding scheme.
   *
   * @param maxBits maximum allowed bit width for the decoded value. Not validated when {@code
   *     maxBits == 64}, since a 64-bit accumulator can't exceed that width in the first place.
   * @throws ProtocolReadException if the decoded value exceeds {@code maxBits}, or if more
   *     continuation bytes are received than {@code maxBits} could ever require.
   */
  default long readVaruint(int maxBits) {
    long valueMax = maxBits < 64 ? (1L << maxBits) - 1 : -1;
    int byteLimit = (maxBits + 6) / 7; // ceil(maxBits / 7)

    long result = 0;
    int i = 0;
    while (i < byteLimit) {
      int b = readUnsignedByte();
      result |= (long) (b & 0x7F) << (7 * i);

      if (maxBits < 64 && (result < 0 || result > valueMax)) {
        throw new ProtocolReadException(
            "Received varint was outside the range of " + maxBits + "-bit int.");
      }

      if ((b & 0x80) == 0) {
        return result;
      }
      i++;
    }
    throw new ProtocolReadException(
        "Received varint had too many bytes for "
            + maxBits
            + "-bit int (continuation bit set on byte "
            + byteLimit
            + ").");
  }

  /** Read a 32-bit signed integer using a variable-length encoding. See {@link #readVaruint}. */
  default int readVarint() {
    long unsigned = readVaruint(32);
    return (int) ByteIoUtils.fromTwosComplement(unsigned, 32);
  }

  /** Read a 64-bit signed integer using a variable-length encoding. See {@link #readVaruint}. */
  default long readVarlong() {
    long unsigned = readVaruint(64);
    return ByteIoUtils.fromTwosComplement(unsigned, 64);
  }

  /**
   * Read a sequence of bytes prefixed with its length encoded as a varint.
   *
   * @throws ProtocolReadException if the encoded length prefix is negative.
   */
  default byte[] readByteArray() {
    int length = readVarint();
    if (length < 0) {
      throw new ProtocolReadException(
          "Length prefix for byte arrays must be non-negative, got " + length + ".");
    }
    return read(length);
  }

  /**
   * Read an ISO-8859-1 encoded string, until a NUL (0x00) terminator (excluded from the result).
   */
  default String readAscii() {
    java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
    byte last;
    do {
      last = read(1)[0];
      if (last != 0) {
        result.write(last);
      }
    } while (last != 0);
    return result.toString(StandardCharsets.ISO_8859_1);
  }

  /**
   * Read a UTF-8 encoded string prefixed with its byte length as a varint.
   *
   * @throws ProtocolReadException if the length prefix is negative, exceeds 131068 bytes, or the
   *     decoded string exceeds 32767 characters (data is still fully read in that last case,
   *     mirroring Minecraft's own implementation).
   */
  default String readUtf() {
    int length = readVarint();
    if (length < 0) {
      throw new ProtocolReadException(
          "Length prefix for utf strings must be non-negative, got " + length + ".");
    }
    if (length > 131_068) {
      throw new ProtocolReadException(
          "Maximum read limit for utf strings is 131068 bytes, got " + length + ".");
    }

    byte[] data = read(length);
    String chars = new String(data, StandardCharsets.UTF_8);

    if (chars.codePointCount(0, chars.length()) > 32_767) {
      throw new ProtocolReadException(
          "Maximum read limit for utf strings is 32767 characters, got "
              + chars.codePointCount(0, chars.length())
              + ".");
    }

    return chars;
  }

  /**
   * Read a boolean indicating whether a value is present and, if so, deserialize it via {@code
   * reader}.
   *
   * @return {@code null} if absent, otherwise {@code reader}'s result.
   */
  default <R> R readOptional(Supplier<R> reader) {
    if (!readBool()) {
      return null;
    }
    return reader.get();
  }
}
