package de.brentspine.mcstatus4j.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

/**
 * Ported from Python mcstatus's {@code tests/net/test_address.py}
 * (validity/constructing/IP-resolving parts).
 */
class AddressTest {

  @Test
  void constructorValidatesPortRange() {
    assertThrows(IllegalArgumentException.class, () -> new Address("example.org", 100_000));
    assertThrows(IllegalArgumentException.class, () -> new Address("example.org", -1));
  }

  @Test
  void constructorAllowsBoundaryPorts() {
    new Address("example.org", 0);
    new Address("example.org", 65535);
  }

  @Test
  void constructorAllowsBareIpv6Host() {
    // Address itself doesn't validate host *format* - only parseAddress's string parser does.
    Address addr = new Address("2345:0425:2CA1:0000:0000:0567:5673:23b5", 100);
    assertEquals("2345:0425:2CA1:0000:0000:0567:5673:23b5", addr.host());
  }

  @Test
  void hostAndPortAccessors() {
    Address addr = new Address("example.org", 25565);
    assertEquals("example.org", addr.host());
    assertEquals(25565, addr.port());
  }

  @Test
  void fromTupleConstructor() {
    Address addr = Address.fromTuple("example.org", 12345);
    assertEquals("example.org", addr.host());
    assertEquals(12345, addr.port());
  }

  @Test
  void parseAddressWithPortNoDefault() {
    Address addr = Address.parseAddress("example.org:25565");
    assertEquals("example.org", addr.host());
    assertEquals(25565, addr.port());
  }

  @Test
  void parseAddressWithPortAndDefaultPrefersExplicitPort() {
    Address addr = Address.parseAddress("example.org:25565", 12345);
    assertEquals("example.org", addr.host());
    assertEquals(25565, addr.port());
  }

  @Test
  void parseAddressWithoutPortUsesDefault() {
    Address addr = Address.parseAddress("example.org", 12345);
    assertEquals("example.org", addr.host());
    assertEquals(12345, addr.port());
  }

  @Test
  void parseAddressWithoutPortAndNoDefaultThrows() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Address.parseAddress("example.org"));
    assertEquals(
        "Given address 'example.org' doesn't contain port and default_port wasn't specified, can't parse.",
        ex.getMessage());
  }

  @Test
  void parseAddressWithInvalidPortThrows() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> Address.parseAddress("example.org:port"));
    assertEquals("Port could not be cast to integer value as 'port'", ex.getMessage());
  }

  @Test
  void parseAddressWithMultiplePortsThrows() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> Address.parseAddress("example.org:12345:25565"));
    assertEquals("Port could not be cast to integer value as '12345:25565'", ex.getMessage());
  }

  @Test
  void parseAddressWithInvalidHostFormatThrows() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Address.parseAddress("hello@#"));
    assertEquals("Invalid address 'hello@#', can't parse.", ex.getMessage());
  }

  @Test
  void parseAddressWithBracketedIpv6AndPort() {
    Address addr = Address.parseAddress("[::1]:25565");
    assertEquals("::1", addr.host());
    assertEquals(25565, addr.port());
  }

  private static Message withAnswer(Message query, org.xbill.DNS.Record answer) {
    Message response = new Message(query.getHeader().getID());
    response.getHeader().setFlag(Flags.QR);
    response.getHeader().setRcode(Rcode.NOERROR);
    response.addRecord(query.getQuestion(), Section.QUESTION);
    response.addRecord(answer, Section.ANSWER);
    return response;
  }

  @Test
  void resolveIpWithHostnameUsesDnsResolver() throws Exception {
    Address addr = new Address("example.org", 25565);
    Name qname = Name.fromString("example.org.", Name.root);
    ARecord aRecord = new ARecord(qname, DClass.IN, 3600, InetAddress.getByName("48.225.1.104"));
    FakeDnsResolver resolver = new FakeDnsResolver(query -> withAnswer(query, aRecord));

    InetAddress resolved = addr.resolveIp(resolver);
    assertEquals("48.225.1.104", resolved.getHostAddress());
  }

  @Test
  void resolveIpCachesResult() throws Exception {
    Address addr = new Address("example.org", 25565);
    Name qname = Name.fromString("example.org.", Name.root);
    ARecord aRecord = new ARecord(qname, DClass.IN, 3600, InetAddress.getByName("48.225.1.104"));
    int[] callCount = {0};
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              callCount[0]++;
              return withAnswer(query, aRecord);
            });

    InetAddress first = addr.resolveIp(resolver);
    InetAddress second = addr.resolveIp(resolver);
    assertSame(first, second);
    assertEquals(1, callCount[0]);
  }

  @Test
  void resolveIpWithIpv4LiteralSkipsDns() throws Exception {
    Address addr = new Address("1.1.1.1", 25565);
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              throw new AssertionError("DNS should not be queried for an IP literal");
            });

    InetAddress resolved = addr.resolveIp(resolver);
    assertEquals("1.1.1.1", resolved.getHostAddress());
    assertEquals(Inet4Address.class, resolved.getClass());
  }

  @Test
  void resolveIpWithIpv6LiteralSkipsDns() throws Exception {
    Address addr = new Address("::1", 25565);
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              throw new AssertionError("DNS should not be queried for an IP literal");
            });

    InetAddress resolved = addr.resolveIp(resolver);
    assertEquals(Inet6Address.class, resolved.getClass());
  }
}
