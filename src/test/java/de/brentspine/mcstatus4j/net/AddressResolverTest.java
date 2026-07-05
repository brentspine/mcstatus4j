package de.brentspine.mcstatus4j.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;

/**
 * Ported from Python mcstatus's {@code tests/net/test_address.py::TestSRVLookup} (SRV fallback
 * logic).
 */
class AddressResolverTest {

  private static Message emptyAnswer(Message query, int rcode) {
    Message response = new Message(query.getHeader().getID());
    response.getHeader().setFlag(Flags.QR);
    response.getHeader().setRcode(rcode);
    response.addRecord(query.getQuestion(), Section.QUESTION);
    return response;
  }

  @Test
  void addressWithExplicitPortSkipsSrvLookup() throws Exception {
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              throw new AssertionError("SRV lookup should not happen when a port is already given");
            });

    Address addr = AddressResolver.lookupJavaAddress("example.org:12345", 25565, resolver);
    assertEquals("example.org", addr.host());
    assertEquals(12345, addr.port());
  }

  @Test
  void addressWithoutPortFallsBackToDefaultOnNxdomain() throws IOException {
    FakeDnsResolver resolver = new FakeDnsResolver(query -> emptyAnswer(query, Rcode.NXDOMAIN));

    Address addr = AddressResolver.lookupJavaAddress("example.org", 25565, resolver);
    assertEquals("example.org", addr.host());
    assertEquals(25565, addr.port());
  }

  @Test
  void addressWithoutPortAndNoDefaultThrowsOnNxdomain() {
    FakeDnsResolver resolver = new FakeDnsResolver(query -> emptyAnswer(query, Rcode.NXDOMAIN));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> AddressResolver.lookupJavaAddress("example.org", null, resolver));
    assertEquals(
        "Given address 'example.org' doesn't contain port, doesn't have an SRV record pointing to a port, and"
            + " default_port wasn't specified, can't parse.",
        ex.getMessage());
  }

  @Test
  void addressWithoutPortUsesSrvRecord() throws Exception {
    Name qname = Name.fromString("_minecraft._tcp.example.org.", Name.root);
    Name target = Name.fromString("different.example.org.", Name.root);
    SRVRecord srv = new SRVRecord(qname, DClass.IN, 3600, 0, 0, 12345, target);
    FakeDnsResolver resolver =
        new FakeDnsResolver(
            query -> {
              Message response = new Message(query.getHeader().getID());
              response.getHeader().setFlag(Flags.QR);
              response.getHeader().setRcode(Rcode.NOERROR);
              response.addRecord(query.getQuestion(), Section.QUESTION);
              response.addRecord(srv, Section.ANSWER);
              return response;
            });

    Address addr = AddressResolver.lookupJavaAddress("example.org", null, resolver);
    assertEquals("different.example.org", addr.host());
    assertEquals(12345, addr.port());
  }
}
