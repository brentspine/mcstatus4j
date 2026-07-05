package de.brentspine.mcstatus4j.protocol.io;

/**
 * Raised when a read operation cannot produce the requested data: not enough bytes were available
 * (mirrors a short socket read), or the decoded value violates a format constraint (e.g. a varint
 * exceeding its bit-width limit, or a string exceeding its maximum length).
 *
 * <p>This is the mcstatus4j equivalent of Python mcstatus's blanket use of {@code OSError} for
 * these failures.
 */
public class ProtocolReadException extends RuntimeException {

  public ProtocolReadException(String message) {
    super(message);
  }

  public ProtocolReadException(String message, Throwable cause) {
    super(message, cause);
  }
}
