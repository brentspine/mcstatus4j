# mcstatus4j

A Java library for querying Minecraft servers (Java and Bedrock editions) via the Server List Ping, Legacy SLP, GameSpy4 Query, and RakNet Unconnected-Ping protocols.

This is a from-scratch Java port of the Python [mcstatus](https://github.com/py-mine/mcstatus) library, maintaining the same layered architecture and wire-protocol fidelity.

## Requirements

- Java 21 or later

## Quick Start

### Java Edition Server (Modern, 1.7+)

```java
import de.brentspine.mcstatus4j.JavaServer;
import de.brentspine.mcstatus4j.responses.JavaStatusResponse;

// Direct connection
JavaServer server = new JavaServer("mc.hypixel.net");
JavaStatusResponse status = server.status();

System.out.println("MOTD: " + status.motd().toPlain());
System.out.println("Players: " + status.players().online() + "/" + status.players().max());
System.out.println("Version: " + status.version().name());
System.out.println("Latency: " + status.latency() + "ms");

// Or use SRV record resolution (mimics Minecraft client behavior)
JavaServer server2 = JavaServer.lookup("hypixel.net");
```

### Async Operations

Every blocking method has an `*Async` counterpart returning a `CompletableFuture`:

```java
CompletableFuture<JavaStatusResponse> futureStatus = server.statusAsync();
CompletableFuture<Double> futurePing = server.pingAsync();

futureStatus.thenAccept(status -> {
    System.out.println("Got status: " + status.motd().toPlain());
});
```

### Legacy Server (Pre-1.7)

```java
import de.brentspine.mcstatus4j.LegacyServer;
import de.brentspine.mcstatus4j.responses.LegacyStatusResponse;

LegacyServer server = new LegacyServer("old.server.com", 25565);
LegacyStatusResponse status = server.status();
```

### Bedrock Edition Server

```java
import de.brentspine.mcstatus4j.BedrockServer;
import de.brentspine.mcstatus4j.responses.BedrockStatusResponse;

BedrockServer server = new BedrockServer("play.cubecraft.net", 19132);
BedrockStatusResponse status = server.status();
```

### Query Protocol (requires `enable-query=true` in server.properties)

```java
import de.brentspine.mcstatus4j.responses.QueryResponse;

JavaServer server = new JavaServer("localhost");
QueryResponse response = server.query();

System.out.println("Map: " + response.map());
System.out.println("Players: " + String.join(", ", response.players().list()));
```

### Racing Java vs Bedrock

```java
import java.util.concurrent.CompletableFuture;

JavaServer javaServer = new JavaServer("example.org");
BedrockServer bedrockServer = new BedrockServer("example.org", 19132);

CompletableFuture<JavaStatusResponse> javaFuture = javaServer.statusAsync();
CompletableFuture<BedrockStatusResponse> bedrockFuture = bedrockServer.statusAsync();

CompletableFuture.anyOf(javaFuture, bedrockFuture).thenAccept(result -> {
    if (result instanceof JavaStatusResponse javaStatus) {
        System.out.println("Java server responded first: " + javaStatus.motd().toPlain());
    } else if (result instanceof BedrockStatusResponse bedrockStatus) {
        System.out.println("Bedrock server responded first: " + bedrockStatus.motd().toPlain());
    }
});
```

## MOTD Formatting

The `Motd` class can render Minecraft's rich-text formatting in multiple output formats:

```java
JavaStatusResponse status = server.status();
Motd motd = status.motd();

String plain = motd.toPlain();          // Strip all formatting
String minecraft = motd.toMinecraft();  // §-codes (§4Red §lBold)
String ansi = motd.toAnsi();           // ANSI escape codes for terminals
String html = motd.toHtml();           // HTML with <p> wrapper
```

## Build from Source

```bash
./gradlew build
```

Run tests:
```bash
./gradlew test
```

Check coverage (requires ≥80% branch coverage):
```bash
./gradlew check
```

## Deviations from Python mcstatus

mcstatus4j maintains structural parallelism with the Python library to ease porting of upstream changes, but adapts idiomatically to Java where appropriate:

1. **Single blocking implementation + CompletableFuture async** — Python maintains hand-duplicated sync/async protocol clients because `asyncio` requires truly non-blocking socket calls. Java uses virtual threads to wrap the single blocking implementation in `CompletableFuture.supplyAsync(...)`, avoiding code duplication while preserving the async API surface.

2. **`TextComponent` wrapper for plain strings** — Python's `ParsedMotdComponent` type alias can include bare `str` as a union member. Java can't declare `java.lang.String` as a permitted subtype of a sealed interface, so plain text is wrapped in a `TextComponent(String value)` record instead.

3. **Deferred scope** — Forge/modded-server metadata parsing (`responses/forge.py` in Python) and the CLI (`mcstatus/__main__.py`) are intentionally omitted from v1 and planned for future milestones.

See [CLAUDE.md](CLAUDE.md) for full architectural context.

## License

MIT License - see LICENSE file for details.
