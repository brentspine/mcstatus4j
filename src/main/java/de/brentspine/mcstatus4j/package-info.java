/**
 * Java library for querying Minecraft servers (Java and Bedrock editions) via Server List Ping,
 * Legacy SLP, GameSpy4 Query, and RakNet Unconnected-Ping protocols.
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * // Java edition server (modern, 1.7+)
 * JavaServer server = new JavaServer("mc.hypixel.net");
 * JavaStatusResponse status = server.status();
 * System.out.println("Players: " + status.players().online() + "/" + status.players().max());
 *
 * // Bedrock edition server
 * BedrockServer bedrock = new BedrockServer("play.cubecraft.net", 19132);
 * BedrockStatusResponse bedrockStatus = bedrock.status();
 *
 * // Async operations (backed by virtual threads)
 * CompletableFuture<JavaStatusResponse> futureStatus = server.statusAsync();
 * }</pre>
 *
 * <h2>Main Entry Points</h2>
 *
 * <ul>
 *   <li>{@link de.brentspine.mcstatus4j.JavaServer} — Modern Java edition servers (1.7+), supports
 *       status/ping/query
 *   <li>{@link de.brentspine.mcstatus4j.LegacyServer} — Pre-1.7 Java edition servers
 *   <li>{@link de.brentspine.mcstatus4j.BedrockServer} — Bedrock edition servers
 * </ul>
 *
 * <h2>Response Types</h2>
 *
 * All in {@link de.brentspine.mcstatus4j.responses}:
 *
 * <ul>
 *   <li>{@link de.brentspine.mcstatus4j.responses.JavaStatusResponse}
 *   <li>{@link de.brentspine.mcstatus4j.responses.LegacyStatusResponse}
 *   <li>{@link de.brentspine.mcstatus4j.responses.BedrockStatusResponse}
 *   <li>{@link de.brentspine.mcstatus4j.responses.QueryResponse}
 * </ul>
 *
 * <h2>MOTD Formatting</h2>
 *
 * The {@link de.brentspine.mcstatus4j.motd.Motd} class renders Minecraft's rich-text formatting in
 * multiple output formats:
 *
 * <pre>{@code
 * Motd motd = status.motd();
 * String plain = motd.toPlain();          // Strip all formatting
 * String minecraft = motd.toMinecraft();  // §-codes (§4Red §lBold)
 * String ansi = motd.toAnsi();           // ANSI escape codes for terminals
 * String html = motd.toHtml();           // HTML with <p> wrapper
 * }</pre>
 *
 * @see de.brentspine.mcstatus4j.JavaServer
 * @see de.brentspine.mcstatus4j.LegacyServer
 * @see de.brentspine.mcstatus4j.BedrockServer
 */
package de.brentspine.mcstatus4j;
