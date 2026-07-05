package de.brentspine.mcstatus4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.brentspine.mcstatus4j.responses.JavaStatusResponse;
import de.brentspine.mcstatus4j.responses.QueryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Real-loopback-socket integration tests for {@link JavaServer}, exercising {@link
 * de.brentspine.mcstatus4j.protocol.io.TcpConnection}/{@link
 * de.brentspine.mcstatus4j.protocol.io.UdpConnection} end-to-end (unlike {@code JavaClientTest},
 * which only ever talks to a {@code FakeConnection}). Mirrors the one real-socket test in Python
 * mcstatus's {@code tests/test_server.py} (a loopback {@code asyncio.Server}).
 */
class JavaServerTest {

  private static final HexFormat HEX = HexFormat.of();

  @Test
  void defaultPortIs25565() {
    JavaServer server = new JavaServer("127.0.0.1");
    assertEquals(25565, server.address().port());
  }

  @Test
  void queryPortDefaultsToPort() {
    JavaServer server = new JavaServer("127.0.0.1", 12345);
    assertEquals(12345, server.queryPort());
  }

  @Test
  void queryPortCanDiffer() {
    JavaServer server = new JavaServer("127.0.0.1", 25565, McServer.defaultTimeout(), 25566);
    assertEquals(25566, server.queryPort());
  }

  @Test
  void queryPortValidatesRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new JavaServer("127.0.0.1", 25565, McServer.defaultTimeout(), 100_000));
  }

  @Test
  void pingOverRealLoopbackSocket() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();

      CompletableFuture<Void> serverTask =
          CompletableFuture.runAsync(
              () -> {
                try (Socket clientSocket = serverSocket.accept()) {
                  InputStream in = clientSocket.getInputStream();
                  OutputStream out = clientSocket.getOutputStream();
                  byte[] buffer = new byte[256];
                  int read = in.read(buffer);
                  if (read <= 0) {
                    throw new IllegalStateException("Expected the client to send a request");
                  }
                  out.write(HEX.parseHex("09010000000001C54246"));
                  out.flush();
                  // Wait for client to close connection to avoid RST race on Linux
                  in.read();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });

      JavaServer server = new JavaServer("127.0.0.1", port, Duration.ofSeconds(3));
      double latency = server.ping(1, 47, 29704774L);

      serverTask.get(5, TimeUnit.SECONDS);
      assertTrue(latency >= 0);
    }
  }

  @Test
  void statusOverRealLoopbackSocket() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();

      CompletableFuture<Void> serverTask =
          CompletableFuture.runAsync(
              () -> {
                try (Socket clientSocket = serverSocket.accept()) {
                  InputStream in = clientSocket.getInputStream();
                  OutputStream out = clientSocket.getOutputStream();
                  byte[] buffer = new byte[256];
                  int read = in.read(buffer);
                  if (read <= 0) {
                    throw new IllegalStateException("Expected the client to send a request");
                  }
                  out.write(
                      HEX.parseHex(
                          "6D006B7B226465736372697074696F6E223A2241204D696E65637261667420536572766572222C227"
                              + "06C6179657273223A7B226D6178223A32302C226F6E6C696E65223A307D2C2276657273696F6E223A7B22"
                              + "6E616D65223A22312E38222C2270726F746F636F6C223A34377D7D"));
                  out.flush();
                  // Wait for client to close connection to avoid RST race on Linux
                  in.read();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });

      JavaServer server = new JavaServer("127.0.0.1", port, Duration.ofSeconds(3));
      JavaStatusResponse status = server.status(1, 47, null);

      serverTask.get(5, TimeUnit.SECONDS);
      assertEquals("A Minecraft Server", status.raw().get("description"));
      assertTrue(status.latency() >= 0);
    }
  }

  @Test
  void asyncPingCompletesOverRealLoopbackSocket() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();

      CompletableFuture<Void> serverTask =
          CompletableFuture.runAsync(
              () -> {
                try (Socket clientSocket = serverSocket.accept()) {
                  InputStream in = clientSocket.getInputStream();
                  OutputStream out = clientSocket.getOutputStream();
                  byte[] buffer = new byte[256];
                  in.read(buffer);
                  out.write(HEX.parseHex("09010000000001C54246"));
                  out.flush();
                  // Wait for client to close connection to avoid RST race on Linux
                  in.read();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });

      JavaServer server = new JavaServer("127.0.0.1", port, Duration.ofSeconds(3));
      double latency = server.pingAsync(1, 47, 29704774L).get(5, TimeUnit.SECONDS);

      serverTask.get(5, TimeUnit.SECONDS);
      assertTrue(latency >= 0);
    }
  }

  @Test
  void queryOverRealLoopbackSocket() throws Exception {
    // QueryClient's response parsing only ever strips a fixed 5-byte (packet-type + session-id)
    // prefix - it never validates that prefix against what it sent - so the server fixture below
    // can reply with a fixed dummy 5-byte header rather than echoing the real session id.
    byte[] dummyHeader = new byte[] {0, 0, 0, 0, 0};

    try (DatagramSocket serverSocket = new DatagramSocket(0)) {
      int port = serverSocket.getLocalPort();

      CompletableFuture<Void> serverTask =
          CompletableFuture.runAsync(
              () -> {
                try {
                  byte[] buffer = new byte[256];

                  DatagramPacket handshakeRequest = new DatagramPacket(buffer, buffer.length);
                  serverSocket.receive(handshakeRequest);
                  byte[] challengeResponse =
                      java.nio.ByteBuffer.allocate(dummyHeader.length + "570350778".length() + 1)
                          .put(dummyHeader)
                          .put("570350778".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                          .put((byte) 0)
                          .array();
                  serverSocket.send(
                      new DatagramPacket(
                          challengeResponse,
                          challengeResponse.length,
                          handshakeRequest.getAddress(),
                          handshakeRequest.getPort()));

                  DatagramPacket queryRequest = new DatagramPacket(buffer, buffer.length);
                  serverSocket.receive(queryRequest);
                  // 11 zero bytes stand in for the "splitnum\x00\x80\x00" marker parseResponse
                  // unconditionally skips, followed by the actual key/value + player-list body.
                  byte[] splitnumMarker = new byte[11];
                  byte[] fullStatBody =
                      HEX.parseHex(
                          "686f73746e616d650041204d696e656372616674205365727665720067616d657479706500534d500067616"
                              + "d655f6964004d494e4543524146540076657273696f6e00312e3800706c7567696e7300006d61700077"
                              + "6f726c64006e756d706c61796572730033006d6178706c617965727300323000686f7374706f727400"
                              + "323535363500686f73746970003139322e3136382e35362e31000001706c617965725f0000"
                              + "44696e6e6572626f6e6500446a696e6e69626f6e650053746576650000");
                  byte[] queryResponse =
                      java.nio.ByteBuffer.allocate(
                              dummyHeader.length + splitnumMarker.length + fullStatBody.length)
                          .put(dummyHeader)
                          .put(splitnumMarker)
                          .put(fullStatBody)
                          .array();
                  serverSocket.send(
                      new DatagramPacket(
                          queryResponse,
                          queryResponse.length,
                          queryRequest.getAddress(),
                          queryRequest.getPort()));
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });

      JavaServer server = new JavaServer("127.0.0.1", 25565, Duration.ofSeconds(3), port);
      QueryResponse response = server.query(1);

      serverTask.get(5, TimeUnit.SECONDS);
      assertEquals("A Minecraft Server", response.raw().get("hostname"));
    }
  }
}
