package de.brentspine.mcstatus4j.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

/**
 * Ported from Python mcstatus's {@code tests/net/test_address.py::TestSRVLookup}, plus A-record
 * coverage.
 */
class DnsTest {

  private static Name absolute(String name) throws Exception {
    return Name.fromString(name, Name.root);
  }

  private static Message emptyAnswer(Message query, int rcode) {
    Message response = new Message(query.getHeader().getID());
    response.getHeader().setFlag(Flags.QR);
    response.getHeader().setRcode(rcode);
    response.addRecord(query.getQuestion(), Section.QUESTION);
    return response;
  }

  private static Message withAnswer(Message query, Record answer) {
    Message response = emptyAnswer(query, Rcode.NOERROR);
    response.addRecord(answer, Section.ANSWER);
    return response;
  }

  @Test
  void resolveARecordReturnsAddress() throws Exception {
    Name qname = absolute("example.org.");
    ARecord aRecord = new ARecord(qname, DClass.IN, 3600, InetAddress.getByName("48.225.1.104"));
    FakeDnsResolver resolver = new FakeDnsResolver(query -> withAnswer(query, aRecord));

    InetAddress resolved = Dns.resolveARecord("example.org", resolver);
    assertEquals("48.225.1.104", resolved.getHostAddress());
  }

  @Test
  void resolveARecordThrowsOnNxdomain() {
    FakeDnsResolver resolver = new FakeDnsResolver(query -> emptyAnswer(query, Rcode.NXDOMAIN));
    assertThrows(IOException.class, () -> Dns.resolveARecord("example.org", resolver));
  }

  @Test
  void resolveSrvRecordReturnsTarget() throws Exception {
    Name qname = absolute("_minecraft._tcp.example.org.");
    Name target = absolute("different.example.org.");
    SRVRecord srv = new SRVRecord(qname, DClass.IN, 3600, 0, 0, 12345, target);
    FakeDnsResolver resolver = new FakeDnsResolver(query -> withAnswer(query, srv));

    Optional<Dns.SrvTarget> result = Dns.resolveSrvRecord("_minecraft._tcp.example.org", resolver);
    assertTrue(result.isPresent());
    assertEquals("different.example.org", result.get().host());
    assertEquals(12345, result.get().port());
  }

  @Test
  void resolveSrvRecordEmptyOnNxdomain() throws IOException {
    FakeDnsResolver resolver = new FakeDnsResolver(query -> emptyAnswer(query, Rcode.NXDOMAIN));
    assertFalse(Dns.resolveSrvRecord("_minecraft._tcp.example.org", resolver).isPresent());
  }

  @Test
  void resolveSrvRecordEmptyOnNoAnswer() throws IOException {
    // NOERROR with an empty answer section is dnsjava's TYPE_NOT_FOUND (Python's "NoAnswer").
    FakeDnsResolver resolver = new FakeDnsResolver(query -> emptyAnswer(query, Rcode.NOERROR));
    assertFalse(Dns.resolveSrvRecord("_minecraft._tcp.example.org", resolver).isPresent());
  }

  @Test
  void resolveMcSrvUsesMinecraftServiceName() throws Exception {
    Name qname = absolute("_minecraft._tcp.example.org.");
    Name target = absolute("different.example.org.");
    SRVRecord srv = new SRVRecord(qname, DClass.IN, 3600, 0, 0, 12345, target);
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              assertEquals(qname, query.getQuestion().getName());
              assertEquals(Type.SRV, query.getQuestion().getType());
              return withAnswer(query, srv);
            });

    Optional<Dns.SrvTarget> result = Dns.resolveMcSrv("example.org", resolver);
    assertTrue(result.isPresent());
    assertEquals("different.example.org", result.get().host());
    assertEquals(12345, result.get().port());
  }
}
