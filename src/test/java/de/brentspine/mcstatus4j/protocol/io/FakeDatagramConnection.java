package de.brentspine.mcstatus4j.protocol.io;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An in-memory stand-in for {@link UdpConnection}: queues whole datagrams for a client under test
 * to read one at a time, and captures whatever it writes.
 *
 * <p>Mirrors Python mcstatus's {@code tests/protocol/helpers.py: SyncDatagramConnection}.
 */
public final class FakeDatagramConnection implements Connection {

  private final Buffer sent = new Buffer();
  private final Deque<byte[]> received = new ArrayDeque<>();

  /** Queue one datagram for the client under test to read next. */
  public void receive(byte[] data) {
    received.add(data);
  }

  @Override
  public byte[] read(int length) {
    byte[] next = received.poll();
    if (next == null) {
      throw new ProtocolReadException("No datagram data to read.");
    }
    return next;
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
