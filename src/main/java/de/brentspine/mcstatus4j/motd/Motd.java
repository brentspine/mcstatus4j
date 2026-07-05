package de.brentspine.mcstatus4j.motd;

import de.brentspine.mcstatus4j.motd.transform.AnsiTransformer;
import de.brentspine.mcstatus4j.motd.transform.HtmlTransformer;
import de.brentspine.mcstatus4j.motd.transform.MinecraftTransformer;
import de.brentspine.mcstatus4j.motd.transform.PlainTransformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed MOTD (message of the day), transformable into plain text, Minecraft {@code §}-code,
 * HTML, or ANSI representations.
 *
 * <p>Mirrors Python mcstatus's {@code Motd}. {@code raw} is loosely typed ({@code String}, {@code
 * Map<String,Object>}, or {@code List<Object>} - whatever the JSON/chat-component payload actually
 * was) since the underlying format is itself a loosely-typed recursive chat-component tree.
 */
public record Motd(List<MotdComponent> parsed, Object raw, boolean bedrock) {

  private static final Pattern MOTD_COLORS_RE =
      Pattern.compile("([§|&][0-9A-Z])", Pattern.CASE_INSENSITIVE);

  /**
   * Parse a raw MOTD value (as it comes directly off the wire: a JSON chat-component tree, or a
   * legacy {@code §}/{@code &}-coded string) into a {@link Motd}.
   *
   * @param bedrock whether this is a Bedrock edition server (affects which color/formatting codes
   *     are recognized; doesn't otherwise change parsing).
   * @throws IllegalArgumentException if {@code raw} isn't a {@code String}, {@code Map}, or {@code
   *     List}.
   */
  public static Motd parse(Object raw, boolean bedrock) {
    Object dictOrStr = raw;
    if (raw instanceof List<?> list) {
      dictOrStr = Map.of("extra", list);
    }

    List<MotdComponent> parsed;
    if (dictOrStr instanceof String s) {
      parsed = parseAsString(s, bedrock);
    } else if (dictOrStr instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> typed = (Map<String, Object>) map;
      parsed = parseAsDict(typed, bedrock, null);
    } else {
      throw new IllegalArgumentException(
          "Expected list, string or map data, got "
              + (raw == null ? "null" : raw.getClass())
              + " ("
              + raw
              + "), report this!");
    }

    return new Motd(parsed, raw, bedrock);
  }

  private static List<MotdComponent> parseAsString(String raw, boolean bedrock) {
    List<MotdComponent> parsedMotd = new ArrayList<>();

    for (String element : splitWithDelimiters(raw)) {
      if (element.isEmpty()) {
        continue;
      }

      String cleanElement = stripLeadingMarkers(element).toLowerCase(Locale.ROOT);
      String standardizedElement = element.replace('&', '§').toLowerCase(Locale.ROOT);

      if (standardizedElement.startsWith("§")) {
        Optional<AnyMinecraftColor> color = colorFromCode(cleanElement, bedrock);
        if (color.isPresent()) {
          parsedMotd.add(color.get());
          continue;
        }
        Optional<AnyFormatting> formatting = formattingFromCode(cleanElement, bedrock);
        if (formatting.isPresent()) {
          parsedMotd.add(formatting.get());
          continue;
        }
        parsedMotd.add(new InvalidFormatting(cleanElement));
      } else {
        parsedMotd.add(new TextComponent(element));
      }
    }

    return parsedMotd;
  }

  private static List<MotdComponent> parseAsDict(
      Map<String, Object> item, boolean bedrock, List<MotdComponent> autoAdd) {
    List<MotdComponent> parsedMotd = autoAdd != null ? new ArrayList<>(autoAdd) : new ArrayList<>();

    Object color = item.get("color");
    if (color != null) {
      parsedMotd.add(parseColor(String.valueOf(color), bedrock));
    }

    for (AnyFormatting styleValue : formattingValues(bedrock)) {
      String key = styleValue.name().toLowerCase(Locale.ROOT);
      Object value = item.get(key);
      if (Boolean.FALSE.equals(value)) {
        // Some servers set formatting keys to false even without it ever being set true before.
        parsedMotd.remove(styleValue);
      } else if (value != null) {
        parsedMotd.add(styleValue);
      }
    }

    Object text = item.get("text");
    if (text != null) {
      parsedMotd.addAll(parseAsString(String.valueOf(text), bedrock));
    }
    Object translate = item.get("translate");
    if (translate != null) {
      parsedMotd.add(new TranslationTag(String.valueOf(translate)));
    }
    parsedMotd.add(resetFormatting(bedrock));

    if (item.containsKey("extra")) {
      AnyFormatting reset = resetFormatting(bedrock);
      List<MotdComponent> newAutoAdd = new ArrayList<>();
      for (MotdComponent element : parsedMotd) {
        boolean isThisEditionFormatting =
            bedrock ? element instanceof BedrockFormatting : element instanceof JavaFormatting;
        if (isThisEditionFormatting && !element.equals(reset)) {
          newAutoAdd.add(element);
        }
      }

      Object extraObj = item.get("extra");
      List<?> extraList = (List<?>) extraObj;
      for (Object extraElement : extraList) {
        if (extraElement instanceof Map<?, ?> map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> typed = (Map<String, Object>) map;
          parsedMotd.addAll(parseAsDict(typed, bedrock, new ArrayList<>(newAutoAdd)));
        } else {
          List<MotdComponent> combined = new ArrayList<>(newAutoAdd);
          combined.addAll(parseAsString(String.valueOf(extraElement), bedrock));
          parsedMotd.addAll(combined);
        }
      }
    }

    return parsedMotd;
  }

  private static MotdComponent parseColor(String color, boolean bedrock) {
    try {
      return colorFromName(color.toUpperCase(Locale.ROOT), bedrock);
    } catch (IllegalArgumentException notANamedColor) {
      if ("reset".equals(color)) {
        // Minecraft servers can't actually return {"reset": true} - instead reset is sent as a
        // "color" value ({"color": "reset"}). Logically reset is a formatting though (it clears
        // both color and other formatting), so we represent it as such.
        return resetFormatting(bedrock);
      }
      try {
        return WebColor.fromHex(color);
      } catch (IllegalArgumentException notAWebColorEither) {
        throw new IllegalArgumentException(
            "Unable to parse color: '" + color + "', report this!", notAWebColorEither);
      }
    }
  }

  private static List<String> splitWithDelimiters(String raw) {
    List<String> result = new ArrayList<>();
    Matcher matcher = MOTD_COLORS_RE.matcher(raw);
    int lastEnd = 0;
    while (matcher.find()) {
      result.add(raw.substring(lastEnd, matcher.start()));
      result.add(matcher.group(1));
      lastEnd = matcher.end();
    }
    result.add(raw.substring(lastEnd));
    return result;
  }

  private static String stripLeadingMarkers(String s) {
    int i = 0;
    while (i < s.length() && (s.charAt(i) == '&' || s.charAt(i) == '§')) {
      i++;
    }
    return s.substring(i);
  }

  private static Optional<AnyMinecraftColor> colorFromCode(String code, boolean bedrock) {
    if (bedrock) {
      return BedrockMinecraftColor.fromCode(code).map(c -> (AnyMinecraftColor) c);
    }
    return JavaMinecraftColor.fromCode(code).map(c -> (AnyMinecraftColor) c);
  }

  private static Optional<AnyFormatting> formattingFromCode(String code, boolean bedrock) {
    if (bedrock) {
      return BedrockFormatting.fromCode(code).map(f -> (AnyFormatting) f);
    }
    return JavaFormatting.fromCode(code).map(f -> (AnyFormatting) f);
  }

  private static AnyMinecraftColor colorFromName(String name, boolean bedrock) {
    if (bedrock) {
      return BedrockMinecraftColor.valueOf(name);
    }
    return JavaMinecraftColor.valueOf(name);
  }

  private static AnyFormatting resetFormatting(boolean bedrock) {
    return bedrock ? BedrockFormatting.RESET : JavaFormatting.RESET;
  }

  private static List<AnyFormatting> formattingValues(boolean bedrock) {
    List<AnyFormatting> result = new ArrayList<>();
    if (bedrock) {
      Collections.addAll(result, BedrockFormatting.values());
    } else {
      Collections.addAll(result, JavaFormatting.values());
    }
    return result;
  }

  /**
   * Return a new {@link Motd} with redundant tokens (duplicate colors, formatting overridden before
   * any text, meaningless resets, etc.) removed, and adjacent text tokens merged.
   */
  public Motd simplify() {
    List<MotdComponent> current = new ArrayList<>(parsed);
    List<MotdComponent> previous = null;

    while (!current.equals(previous)) {
      previous = current;
      Set<Integer> unused = MotdSimplifier.getUnusedElements(current);
      List<MotdComponent> next = new ArrayList<>();
      for (int i = 0; i < current.size(); i++) {
        if (!unused.contains(i)) {
          next.add(current.get(i));
        }
      }
      current = next;
    }

    current = MotdSimplifier.squashNearbyStrings(current);
    return new Motd(current, raw, bedrock);
  }

  /**
   * Plain text, with all colors/formatting stripped. E.g. {@code "&0Hello &oWorld"} -> {@code
   * "Hello World"}.
   */
  public String toPlain() {
    return new PlainTransformer(bedrock).transform(parsed);
  }

  /**
   * Minecraft's own {@code §}-code representation (always using {@code §}, even if the source used
   * {@code &}).
   */
  public String toMinecraft() {
    return new MinecraftTransformer(bedrock).transform(parsed);
  }

  /** HTML, always wrapped in a {@code <p>} tag. */
  public String toHtml() {
    return new HtmlTransformer(bedrock).transform(parsed);
  }

  /** 24-bit ANSI terminal escape codes. */
  public String toAnsi() {
    return new AnsiTransformer(bedrock).transform(parsed);
  }
}
