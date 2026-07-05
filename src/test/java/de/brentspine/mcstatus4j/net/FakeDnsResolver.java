package de.brentspine.mcstatus4j.net;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.xbill.DNS.EDNSOption;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TSIG;

/**
 * A dnsjava {@link Resolver} that answers every query via a caller-supplied function, so DNS
 * lookups can be tested without touching the network. Mirrors the role of Python mcstatus's {@code
 * patch("dns.resolver.resolve")} test fixtures.
 */
final class FakeDnsResolver implements Resolver {

  private final Function<Message, Message> responder;

  FakeDnsResolver(Function<Message, Message> responder) {
    this.responder = responder;
  }

  @Override
  public Message send(Message query) throws IOException {
    return responder.apply(query);
  }

  @Override
  public void setPort(int port) {}

  @Override
  public void setTCP(boolean flag) {}

  @Override
  public void setIgnoreTruncation(boolean flag) {}

  @Override
  public void setEDNS(int level, int payloadSize, int flags, List<EDNSOption> options) {}

  @Override
  public void setTSIGKey(TSIG key) {}

  @Override
  public void setTimeout(Duration duration) {}
}
