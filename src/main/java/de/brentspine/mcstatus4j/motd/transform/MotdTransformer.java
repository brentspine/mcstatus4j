package de.brentspine.mcstatus4j.motd.transform;

import de.brentspine.mcstatus4j.motd.AnyFormatting;
import de.brentspine.mcstatus4j.motd.AnyMinecraftColor;
import de.brentspine.mcstatus4j.motd.BedrockFormatting;
import de.brentspine.mcstatus4j.motd.InvalidFormatting;
import de.brentspine.mcstatus4j.motd.JavaFormatting;
import de.brentspine.mcstatus4j.motd.MotdComponent;
import de.brentspine.mcstatus4j.motd.TextComponent;
import de.brentspine.mcstatus4j.motd.TranslationTag;
import de.brentspine.mcstatus4j.motd.WebColor;
import java.util.ArrayList;
import java.util.List;

/**
 * Base MOTD transformer: renders a list of {@link MotdComponent}s into an alternative string
 * representation (plain text, Minecraft {@code §}-codes, HTML, ANSI).
 *
 * <p>Mirrors Python mcstatus's {@code _BaseTransformer}/{@code _NothingTransformer}, collapsed into
 * one class since every concrete transformer here produces a {@code String} (Python kept its base
 * generic over both the per-component hook return type and the end result type, but never used
 * anything besides {@code str} for either - a disclosed simplification).
 */
public abstract class MotdTransformer {

  protected final boolean bedrock;

  protected MotdTransformer(boolean bedrock) {
    this.bedrock = bedrock;
  }

  /** Render the given parsed MOTD components into this transformer's output format. */
  public String transform(List<MotdComponent> components) {
    List<String> results = new ArrayList<>();
    for (MotdComponent component : components) {
      results.addAll(handleComponent(component));
    }
    return formatOutput(results);
  }

  protected abstract String formatOutput(List<String> results);

  protected String handleStr(String element) {
    return "";
  }

  protected String handleTranslationTag(TranslationTag tag) {
    return "";
  }

  protected String handleWebColor(WebColor color) {
    return "";
  }

  protected String handleFormatting(AnyFormatting formatting) {
    return "";
  }

  protected String handleInvalidFormatting(InvalidFormatting invalid) {
    return "";
  }

  protected String handleMinecraftColor(AnyMinecraftColor color) {
    return "";
  }

  /**
   * Dispatch one component to its handler. A color token also implicitly emits a formatting RESET
   * immediately before it, mirroring how real Minecraft rendering always clears bold/italic/etc.
   * state when a color is set.
   */
  protected List<String> handleComponent(MotdComponent component) {
    String additional = null;
    if (component instanceof AnyMinecraftColor) {
      additional = handleFormatting(resetFormatting());
    }
    String result = dispatch(component);
    if (additional != null) {
      return List.of(additional, result);
    }
    return List.of(result);
  }

  protected final AnyFormatting resetFormatting() {
    return bedrock ? BedrockFormatting.RESET : JavaFormatting.RESET;
  }

  private String dispatch(MotdComponent component) {
    if (component instanceof AnyMinecraftColor color) {
      return handleMinecraftColor(color);
    }
    if (component instanceof WebColor webColor) {
      return handleWebColor(webColor);
    }
    if (component instanceof AnyFormatting formatting) {
      return handleFormatting(formatting);
    }
    if (component instanceof InvalidFormatting invalid) {
      return handleInvalidFormatting(invalid);
    }
    if (component instanceof TranslationTag tag) {
      return handleTranslationTag(tag);
    }
    if (component instanceof TextComponent text) {
      return handleStr(text.value());
    }
    throw new IllegalStateException("Invalid component type: " + component.getClass());
  }
}
