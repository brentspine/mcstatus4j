package de.brentspine.mcstatus4j.protocol.io;

/**
 * Fixed-width binary value formats used by {@link ProtocolReader#readValue} and {@link
 * ProtocolWriter#writeValue}, always encoded/decoded big-endian.
 *
 * <p>Mirrors Python mcstatus's {@code StructFormat} enum (backed by the {@code struct} module in
 * standard-size, big-endian mode). Not every member here is exercised by mcstatus4j's own protocol
 * clients yet (e.g. {@link #FLOAT}, {@link #DOUBLE}), but the full set is kept for parity with
 * upstream and for future protocol work (e.g. Forge data decoding).
 */
public enum StructFormat {
  BOOL(1),
  CHAR(1),
  BYTE(1),
  UBYTE(1),
  SHORT(2),
  USHORT(2),
  INT(4),
  UINT(4),
  /** 4 bytes, matching C's {@code long} under Python struct's standard-size mode. */
  LONG(4),
  /** 4 bytes, matching C's {@code unsigned long} under Python struct's standard-size mode. */
  ULONG(4),
  FLOAT(4),
  DOUBLE(8),
  HALFFLOAT(2),
  LONGLONG(8),
  ULONGLONG(8);

  private final int byteWidth;

  StructFormat(int byteWidth) {
    this.byteWidth = byteWidth;
  }

  /** Number of bytes this format occupies on the wire. */
  public int byteWidth() {
    return byteWidth;
  }
}
