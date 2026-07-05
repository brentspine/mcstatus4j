package de.brentspine.mcstatus4j.protocol.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * A blocking TCP connection, used by the Java edition (modern and legacy) Server List Ping clients.
 *
 * <p>Mirrors Python mcstatus's {@code TCPSocketConnection}. There's no {@code
 * TCPAsyncSocketConnection} counterpart in this port - see {@link ProtocolReader}'s class docs for
 * why.
 */
public final class TcpConnection implements Connection, AutoCloseable {

  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;

  public TcpConnection(InetSocketAddress address, Duration timeout) {
    try {
      socket = new Socket();
      socket.setTcpNoDelay(true);
      socket.connect(address, toMillis(timeout));
      socket.setSoTimeout(toMillis(timeout));
      input = socket.getInputStream();
      output = socket.getOutputStream();
    } catch (IOException e) {
      throw new ProtocolWriteException("Failed to connect to " + address, e);
    }
  }

  private static int toMillis(Duration timeout) {
    long millis = timeout.toMillis();
    return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
  }

  @Override
  public byte[] read(int length) {
    byte[] result = new byte[length];
    int totalRead = 0;
    try {
      while (totalRead < length) {
        int n = input.read(result, totalRead, length - totalRead);
        if (n < 0) {
          throw new ProtocolReadException("Server did not respond with any information!");
        }
        totalRead += n;
      }
    } catch (IOException e) {
      throw new ProtocolReadException("Server did not respond with any information!", e);
    }
    return result;
  }

  @Override
  public void write(byte[] data) {
    try {
      output.write(data);
      output.flush();
    } catch (IOException e) {
      throw new ProtocolWriteException("Failed to write to socket", e);
    }
  }

  @Override
  public void close() {
    // Tolerates "not connected" the way Python's _SocketConnection.close() tolerates ENOTCONN.
    try {
      socket.shutdownInput();
    } catch (IOException ignored) {
      // Nothing to do: the peer may have already closed its side.
    }
    try {
      socket.shutdownOutput();
    } catch (IOException ignored) {
      // Nothing to do: the peer may have already closed its side.
    }
    try {
      socket.close();
    } catch (IOException e) {
      throw new ProtocolWriteException("Failed to close socket", e);
    }
  }
}
