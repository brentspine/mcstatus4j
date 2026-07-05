package de.brentspine.mcstatus4j.protocol.io;

/**
 * An in-memory stand-in for {@link TcpConnection}, backed by two {@link Buffer}s: one queues bytes
 * "sent" by the server for a client under test to read, the other captures whatever the client
 * writes so tests can assert on it.
 *
 * <p>Mirrors Python mcstatus's {@code tests/protocol/helpers.py: SyncBufferConnection}. This is the
 * foundational test helper for every stream-based protocol client test (Java SLP, Legacy SLP).
 */
public final class FakeConnection implements Connection {

  private final Buffer sent = new Buffer();
  private final Buffer received = new Buffer();

  /** Queue bytes for the client under test to read next. */
  public void receive(byte[] data) {
    received.write(data);
  }

  @Override
  public byte[] read(int length) {
    return received.read(length);
  }

  @Override
  public void write(byte[] data) {
    sent.write(data);
  }

  /** Read and clear everything written by the client under test so far. */
  public byte[] flush() {
    return sent.flush();
  }
}
