package de.brentspine.mcstatus4j.protocol.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;

/**
 * A UDP connection to a single fixed peer address, used by the GameSpy4 Query client.
 *
 * <p>Mirrors Python mcstatus's {@code UDPSocketConnection}. Every datagram is read/written whole:
 * the {@code length} parameter to {@link #read} is ignored (matching Python's own {@code
 * UDPSocketConnection.read}), since UDP has no notion of a partial read.
 */
public final class UdpConnection implements Connection, AutoCloseable {

  /** {@code 2^16 - 1}: the maximum possible UDP datagram payload size. */
  public static final int MAX_DATAGRAM_SIZE = 65535;

  private final DatagramSocket socket;
  private final InetSocketAddress address;

  public UdpConnection(InetSocketAddress address, Duration timeout) {
    this.address = address;
    try {
      socket = new DatagramSocket();
      socket.setSoTimeout(toMillis(timeout));
    } catch (IOException e) {
      throw new ProtocolWriteException("Failed to open UDP socket", e);
    }
  }

  private static int toMillis(Duration timeout) {
    long millis = timeout.toMillis();
    return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
  }

  @Override
  public byte[] read(int length) {
    byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    try {
      byte[] result;
      do {
        socket.receive(packet);
        result = Arrays.copyOf(packet.getData(), packet.getLength());
      } while (result.length == 0);
      return result;
    } catch (IOException e) {
      throw new ProtocolReadException("Server did not respond with any information!", e);
    }
  }

  @Override
  public void write(byte[] data) {
    try {
      socket.send(new DatagramPacket(data, data.length, address));
    } catch (IOException e) {
      throw new ProtocolWriteException("Failed to write to UDP socket", e);
    }
  }

  @Override
  public void close() {
    socket.close();
  }
}
