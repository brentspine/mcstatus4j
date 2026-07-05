package de.brentspine.mcstatus4j.motd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Post-parse cleanup heuristics that identify redundant {@link MotdComponent} tokens (used by
 * {@link Motd#simplify()}). Mirrors Python mcstatus's {@code motd/_simplifies.py}.
 */
final class MotdSimplifier {

  private MotdSimplifier() {}

  /** Union of every removal heuristic below - indices safe to drop from {@code parsed}. */
  static Set<Integer> getUnusedElements(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    toRemove.addAll(getDoubleItems(parsed));
    toRemove.addAll(getDoubleColors(parsed));
    toRemove.addAll(getFormattingBeforeColor(parsed));
    toRemove.addAll(getMeaninglessResetsAndColors(parsed));
    toRemove.addAll(getEndNonText(parsed));
    return toRemove;
  }

  /**
   * Merge adjacent plain-text tokens together, mutating {@code parsed} in place (mirrors Python's
   * mutation-during-iteration approach, but removes indices in ascending order for correctness
   * rather than relying on incidental Python {@code set} iteration order).
   */
  static List<MotdComponent> squashNearbyStrings(List<MotdComponent> parsed) {
    Set<Integer> fillers = new TreeSet<>();
    for (int index = 0; index < parsed.size() - 1; index++) {
      if (!(parsed.get(index) instanceof TextComponent item)) {
        continue;
      }
      if (parsed.get(index + 1) instanceof TextComponent nextItem) {
        parsed.set(index + 1, new TextComponent(item.value() + nextItem.value()));
        fillers.add(index);
      }
    }

    int alreadyRemoved = 0;
    for (int indexToRemove : fillers) {
      parsed.remove(indexToRemove - alreadyRemoved);
      alreadyRemoved++;
    }
    return parsed;
  }

  /** Doubled formatting/color/web-color tokens back-to-back are redundant. */
  static Set<Integer> getDoubleItems(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    for (int index = 0; index < parsed.size() - 1; index++) {
      MotdComponent item = parsed.get(index);
      MotdComponent nextItem = parsed.get(index + 1);
      if (isFormattingColorOrWebColor(item) && item.equals(nextItem)) {
        toRemove.add(index);
      }
    }
    return toRemove;
  }

  /** Only the last of consecutive colors (before any text) actually applies. */
  static Set<Integer> getDoubleColors(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    Integer prevColor = null;
    for (int index = 0; index < parsed.size(); index++) {
      MotdComponent item = parsed.get(index);
      if (item instanceof AnyMinecraftColor || item instanceof WebColor) {
        if (prevColor != null) {
          toRemove.add(prevColor);
        }
        prevColor = index;
      }
      if (item instanceof TextComponent) {
        prevColor = null;
      }
    }
    return toRemove;
  }

  /** Formatting immediately followed by a color (before any text) is pointless. */
  static Set<Integer> getFormattingBeforeColor(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    List<Integer> collected = new ArrayList<>();
    for (int index = 0; index < parsed.size(); index++) {
      MotdComponent item = parsed.get(index);
      if (item instanceof AnyFormatting) {
        collected.add(index);
      }
      if (collected.isEmpty()) {
        continue;
      }
      if (item instanceof TextComponent text && !isBlankNonEmpty(text.value())) {
        collected = new ArrayList<>();
        continue;
      }
      if (item instanceof AnyMinecraftColor || item instanceof WebColor) {
        toRemove.addAll(collected);
        collected = new ArrayList<>();
      }
    }
    return toRemove;
  }

  /** Trailing color/formatting after the very last text token is dead. */
  static Set<Integer> getEndNonText(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    for (int i = parsed.size() - 1; i >= 0; i--) {
      MotdComponent item = parsed.get(i);
      if (item instanceof TextComponent) {
        break;
      }
      if (isFormattingColorOrWebColor(item)) {
        toRemove.add(i);
      }
    }
    return toRemove;
  }

  /** A RESET with nothing active, or repeating the same active color/formatting, is meaningless. */
  static Set<Integer> getMeaninglessResetsAndColors(List<MotdComponent> parsed) {
    Set<Integer> toRemove = new HashSet<>();
    MotdComponent activeColor = null;
    AnyFormatting activeFormatting = null;

    for (int index = 0; index < parsed.size(); index++) {
      MotdComponent item = parsed.get(index);

      if (item instanceof JavaMinecraftColor
          || item instanceof BedrockMinecraftColor
          || item instanceof WebColor) {
        if (item.equals(activeColor)) {
          toRemove.add(index);
        }
        activeColor = item;
        continue;
      }

      if (item instanceof AnyFormatting formatting) {
        if ("RESET".equals(formatting.name())) {
          if (activeColor == null && activeFormatting == null) {
            toRemove.add(index);
            continue;
          }
          activeColor = null;
          activeFormatting = null;
          continue;
        }
        if (formatting.equals(activeFormatting)) {
          toRemove.add(index);
        }
        activeFormatting = formatting;
      }
    }
    return toRemove;
  }

  private static boolean isFormattingColorOrWebColor(MotdComponent item) {
    return item instanceof AnyFormatting
        || item instanceof AnyMinecraftColor
        || item instanceof WebColor;
  }

  private static boolean isBlankNonEmpty(String s) {
    if (s.isEmpty()) {
      return false;
    }
    return s.chars().allMatch(Character::isWhitespace);
  }
}
