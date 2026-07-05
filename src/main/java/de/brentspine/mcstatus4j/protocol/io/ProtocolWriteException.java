package de.brentspine.mcstatus4j.protocol.io;

/**
 * Raised when a write to a real connection (socket) fails at the I/O level. {@link Buffer}'s
 * in-memory writes can't fail this way, so this is only thrown by real transport implementations
 * (e.g. {@link TcpConnection}).
 */
public class ProtocolWriteException extends RuntimeException {

  public ProtocolWriteException(String message, Throwable cause) {
    super(message, cause);
  }
}
