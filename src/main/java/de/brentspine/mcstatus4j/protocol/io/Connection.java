package de.brentspine.mcstatus4j.protocol.io;

/**
 * A bidirectional connection: both a {@link ProtocolReader} and a {@link ProtocolWriter}. Lets
 * protocol clients hold a single field type regardless of whether it's backed by a real socket
 * ({@link TcpConnection}, {@code UdpConnection}) or an in-memory test fake.
 */
public interface Connection extends ProtocolReader, ProtocolWriter {}
