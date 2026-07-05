# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

mcstatus4j is a Java library that queries Minecraft servers (Java and Bedrock editions) for status/ping/query information using the Server List Ping and Query protocols, without requiring the caller to understand those wire protocols.

This is a from-scratch Java port of the Python [mcstatus](https://github.com/py-mine/mcstatus) library (`C:\Users\youbo\IdeaProjects\mcstatus\mcstatus`), maintaining the same layered architecture and wire-protocol fidelity to ease future porting of upstream Python changes.

## Setup

Requires Java 21 or later. Build with Gradle:

```sh
./gradlew build          # Compile, run tests, check coverage
./gradlew test           # Run tests only
./gradlew check          # Run tests + coverage gate (≥80% branch) + Spotless formatting check
./gradlew spotlessApply  # Auto-format code with google-java-format
```

## Architecture

The public entry points are `JavaServer`, `LegacyServer`, and `BedrockServer` in the `de.brentspine.mcstatus4j` package. Each wraps a protocol-specific client and exposes both blocking and async variants of the same operation (e.g. `status()` / `statusAsync()`, `ping()` / `pingAsync()`). The async methods return `CompletableFuture` and are backed by `Executors.newVirtualThreadPerTaskExecutor()` wrapping the blocking implementation — there is no separate non-blocking protocol stack.

Layers, roughly bottom-up:

- **`de.brentspine.mcstatus4j.net`** — `Address` (host/port with validation, memoizes `resolveIp()`) and SRV-record-aware DNS lookup (`AddressResolver.lookupJavaAddress(...)` using dnsjava), used by `*Server.lookup()` to mimic how the Minecraft client resolves addresses.

- **`de.brentspine.mcstatus4j.protocol.io`** — low-level binary I/O primitives shared by every protocol client:
  - `ProtocolReader` / `ProtocolWriter`: interfaces with shared primitives (varint, string, etc.) encoding.
  - `Buffer`: mutable class (not a record) with a read cursor position, implementing both reader/writer interfaces for in-memory packet assembly/parsing.
  - `TcpConnection` / `UdpConnection`: real socket wrappers implementing the same reader/writer interfaces.
  - `StructFormat` / `ByteIoUtils`: fixed-width value encoding and two's complement conversions for VarInt/VarLong.

- **`de.brentspine.mcstatus4j.protocol.{java,legacy,query,bedrock}`** — one client per protocol/edition. Unlike Python (which maintains separate sync/async classes), mcstatus4j has a single blocking implementation per client (`JavaClient`, `LegacyClient`, `QueryClient`, `BedrockClient`). The async wrappers live at the `*Server` facade layer.

- **`de.brentspine.mcstatus4j.responses`** — plain record DTOs returned to callers (`JavaStatusResponse`, `BedrockStatusResponse`, `LegacyStatusResponse`, `QueryResponse`). Each has a `static build(...)` factory method that parses raw wire data into the public record. Forge/modded-server metadata fields are intentionally absent (deferred to a future milestone).

- **`de.brentspine.mcstatus4j.motd`** — parses and transforms legacy Minecraft formatting codes (`§`-codes, JSON chat components) into a structured `Motd` object that can be re-rendered as plain text, ANSI, HTML, etc.
  - `Motd`: facade with `parse()`, `simplify()`, `toPlain()`, `toMinecraft()`, `toHtml()`, `toAnsi()`.
  - `MotdComponent`: sealed interface hierarchy (including `TextComponent` wrapper for plain strings — see deviations below).
  - `MotdSimplifier`: package-private collapsing/merging logic.
  - `transform/`: output format transformers (`PlainTransformer`, `MinecraftTransformer`, `HtmlTransformer`, `AnsiTransformer`).

- **`de.brentspine.mcstatus4j.util`** — `Retry.call(int tries, Callable<T> action)`, a functional-style retry helper (Java has no decorator syntax, so call sites wrap their body explicitly instead of annotating a method).

## Key Design Decisions (Intentional Deviations from Python)

These decisions were made deliberately to adapt idiomatically to Java while preserving structural parallelism with the Python codebase:

### 1. Single blocking implementation + CompletableFuture async

**Python** maintains hand-duplicated sync/async protocol clients (`JavaClient` / `AsyncJavaClient`, `LegacyClient` / `AsyncLegacyClient`, etc.) because `asyncio` requires truly non-blocking socket calls — the wire logic is identical, but the I/O layer underneath differs fundamentally.

**Java** uses virtual threads (Java 21+) to make blocking I/O cheap enough that a separate non-blocking implementation isn't needed. mcstatus4j has a single blocking implementation per protocol client, and the `*Async` methods at the `*Server` facade layer wrap those calls in `CompletableFuture.supplyAsync(..., Executors.newVirtualThreadPerTaskExecutor())`.

**Why this preserves portability:** The actual protocol/wire-format code that upstream Python changes (packet structure, field parsing, edge-case handling) lives in the protocol client classes and remains 1:1 comparable between Python and Java. The sync/async *duality* Python needs is an asyncio-specific requirement that Java doesn't share.

### 2. `TextComponent` wrapper for plain strings

**Python's** `ParsedMotdComponent` type alias is `str | JavaFormatting | BedrockFormatting | ...` — a bare `str` can appear directly as a union member.

**Java** can't declare `java.lang.String` as a permitted subtype of a sealed interface (sealed types must be explicitly declared in the same compilation unit). mcstatus4j wraps plain text in a `TextComponent(String value)` record instead, making `MotdComponent` a sealed interface permitting `TextComponent`, `JavaFormatting`, `BedrockFormatting`, etc.

**This is documented prominently** (README, Javadoc) so it isn't mistaken for an oversight when diffing against Python.

### 3. Deferred scope: Forge metadata and CLI

**Forge/modded-server metadata parsing** (`responses/forge.py` in Python) is deferred to a future milestone. This is the single most intricate piece of the Python codebase (a custom 15-bit-per-UTF16-codeunit bit-packing decoder for Forge's old-style server-list payloads) and has zero bearing on basic ping/status/query functionality. The Java `JavaStatusResponse` record has no Forge-related fields yet — they'll be added when Forge support is implemented.

**CLI** (`mcstatus/__main__.py` in Python) is deferred to a future milestone. mcstatus4j v1 is library-only. When the CLI is added, it will live as a separate Gradle submodule with a `picocli` dependency.

## Code Style

- **Spotless** enforces `google-java-format`. Run `./gradlew spotlessApply` before committing.
- **JaCoCo** branch coverage must be ≥80%. Run `./gradlew check` to validate.
- **Test golden packets** use inline `HexFormat.parseHex(...)` hex literals, mirroring Python's `bytes.fromhex(...)` style. No external fixture files.
- **Mutable vs immutable classes:**
  - Use **records** for frozen DTOs (all `*Response`, `*Version`, `*Players` types).
  - Use **classes** when mutability is needed: `Buffer` (has a read cursor), `Address` (memoizes `resolveIp()`).

## Testing

- **JUnit 5** with `@ParameterizedTest` / `@MethodSource` for parametrized cases.
- **`FakeConnection`** and **`FakeDatagramConnection`** in `src/test/java/.../protocol/io/` are the foundational test helpers — every protocol client test uses them to avoid real network calls.
- **Real-socket integration tests** in `JavaServerTest` stand up ephemeral `ServerSocket`/`DatagramSocket` instances on random ports to validate real timeout/TCP_NODELAY/short-read behavior that `FakeConnection` can't exercise.
- **Coverage gaps** in error-handling paths (socket exceptions, DNS failures, timeout branches) are acceptable as long as the 80% gate passes — the happy paths are exercised by real integration tests.

## CI / GitHub Actions

- **`validation.yml`**: Runs `./gradlew spotlessCheck` on every push/PR.
- **`unit-tests.yml`**: Runs `./gradlew test jacocoTestCoverageVerification` on ubuntu/windows/macos.
- **`main.yml`**: Orchestrator that invokes both workflows.

Maven Central publishing and Discord status-embed workflows are intentionally stubbed/omitted for v1 (revisit when Central Portal credentials exist for `de.brentspine`).

## Common Pitfalls

1. **Don't create separate async protocol classes** — the async layer lives at the `*Server` facade level, not in the protocol clients.
2. **Don't use records for mutable state** — `Buffer` and `Address` must stay classes because they have internal mutability.
3. **Check Python's exact regex/format quirks** — e.g. the MOTD color regex `[\xA7|&][0-9A-Z]` has a stray literal `|` in the character class. Replicate Python's behavior exactly, even if it looks wrong (it's cheap to fix later if upstream fixes it).
4. **Two's complement at 64 bits is identity** — Java `long` is already 64-bit two's complement natively, so `ByteIoUtils.toTwosComplement(value, 64)` returns `value` unchanged. The Python codebase needs explicit conversion because Python's `int` is arbitrary-precision.

## Future Work (Not Yet Implemented)

- Forge/modded-server metadata parsing (`responses/forge/` in Python)
- CLI (`mcstatus/__main__.py` in Python, will be a separate Gradle submodule with `picocli`)
- Maven Central publishing (needs credentials for `de.brentspine`)
