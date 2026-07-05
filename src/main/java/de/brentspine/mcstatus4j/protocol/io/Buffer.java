package de.brentspine.mcstatus4j.protocol.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * In-memory growable byte buffer supporting the common read/write operations, with a read cursor
 * that advances independently of the write position.
 *
 * <p>Mirrors Python mcstatus's {@code Buffer} (a {@code bytearray} subclass). Unlike the response
 * DTOs elsewhere in this port, this stays a mutable class rather than a record because of that read
 * cursor.
 */
public final class Buffer implements ProtocolReader, ProtocolWriter {

  private byte[] data;
  private int size;
  private int pos;

  public Buffer() {
    this(new byte[0]);
  }

  public Buffer(byte[] initial) {
    this.data = initial.clone();
    this.size = initial.length;
    this.pos = 0;
  }

  @Override
  public void write(byte[] bytes) {
    ensureCapacity(size + bytes.length);
    System.arraycopy(bytes, 0, data, size, bytes.length);
    size += bytes.length;
  }

  /**
   * Read {@code length} bytes starting at the current position, advancing it. Reading does not
   * remove the data; the next read continues from the first still-unread byte. See {@link #clear}
   * to actually free consumed data.
   *
   * @throws ProtocolReadException if {@code length} is negative, or if fewer than {@code length}
   *     bytes remain unread (mimicking a short read from a real socket; any partially-available
   *     data is still consumed).
   */
  @Override
  public byte[] read(int length) {
    if (length < 0) {
      throw new ProtocolReadException(
          "Requested to read a negative amount of data: " + length + ".");
    }

    int end = pos + length;
    if (end > size) {
      byte[] partial = Arrays.copyOfRange(data, pos, size);
      int bytesRead = size - pos;
      pos = size;
      throw new ProtocolReadException(
          "Requested to read more data than available. Read "
              + bytesRead
              + " bytes: "
              + Arrays.toString(partial)
              + ", out of "
              + length
              + " requested bytes.");
    }

    byte[] result = Arrays.copyOfRange(data, pos, end);
    pos = end;
    return result;
  }

  /** Clear all stored data and reset the position, leaving a blank buffer. */
  public void clear() {
    clear(false);
  }

  /**
   * Clear out the stored data and reset the position.
   *
   * @param onlyAlreadyRead when {@code true}, only the already-read prefix is discarded (the
   *     position resets to the start of the still-unread tail); when {@code false}, all data is
   *     discarded.
   */
  public void clear(boolean onlyAlreadyRead) {
    if (onlyAlreadyRead) {
      int remaining = size - pos;
      System.arraycopy(data, pos, data, 0, remaining);
      size = remaining;
    } else {
      size = 0;
    }
    pos = 0;
  }

  /** Reset the read position to the start, allowing the contained data to be read again. */
  public void reset() {
    pos = 0;
  }

  /** Zero-copy read-only view of the unread portion of the buffer. */
  public ByteBuffer unreadView() {
    return ByteBuffer.wrap(data, pos, size - pos).asReadOnlyBuffer();
  }

  /** Read all remaining data and clear the buffer. */
  public byte[] flush() {
    byte[] result = Arrays.copyOfRange(data, pos, size);
    clear();
    return result;
  }

  /** Number of bytes still unread. */
  public int getRemaining() {
    return size - pos;
  }

  private void ensureCapacity(int minCapacity) {
    if (minCapacity > data.length) {
      int newCapacity = Math.max(data.length * 2, minCapacity);
      data = Arrays.copyOf(data, newCapacity);
    }
  }
}
