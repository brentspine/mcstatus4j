package de.brentspine.mcstatus4j.motd;

/**
 * A single parsed token of a MOTD (message of the day): either plain text, a formatting/color code,
 * an unrecognized formatting code, a hex web color, or an opaque chat-component translation tag.
 *
 * <p>Mirrors Python mcstatus's {@code ParsedMotdComponent} type alias (a union of {@code str} and
 * several small types). Java can't declare {@code java.lang.String} as a permitted subtype of a
 * sealed interface, so plain text is wrapped in {@link TextComponent} instead of using {@link
 * String} directly - a disclosed, intentional deviation from the Python model.
 */
public sealed interface MotdComponent
    permits TextComponent,
        WebColor,
        TranslationTag,
        InvalidFormatting,
        AnyFormatting,
        AnyMinecraftColor {}
