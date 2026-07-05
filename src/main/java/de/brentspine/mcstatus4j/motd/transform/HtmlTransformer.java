package de.brentspine.mcstatus4j.motd.transform;

import de.brentspine.mcstatus4j.motd.AnyFormatting;
import de.brentspine.mcstatus4j.motd.AnyMinecraftColor;
import de.brentspine.mcstatus4j.motd.MotdComponent;
import de.brentspine.mcstatus4j.motd.WebColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a MOTD as HTML, always wrapped in a {@code <p>} tag. Backs {@code Motd.toHtml()}.
 *
 * <p>The "obfuscated" CSS class/animation is left for the caller to implement themselves (see
 * Python mcstatus's {@code to_html} docstring for a reference JS snippet).
 */
public final class HtmlTransformer extends PlainTransformer {

  private static final Map<String, String> FORMATTING_TO_HTML_TAGS =
      Map.of(
          "BOLD", "b",
          "STRIKETHROUGH", "s",
          "ITALIC", "i",
          "UNDERLINED", "u");

  private List<String> onReset = new ArrayList<>();

  public HtmlTransformer(boolean bedrock) {
    super(bedrock);
  }

  @Override
  public String transform(List<MotdComponent> components) {
    onReset = new ArrayList<>();
    return super.transform(components);
  }

  @Override
  protected String formatOutput(List<String> results) {
    return "<p>" + super.formatOutput(results) + String.join("", onReset) + "</p>";
  }

  @Override
  protected String handleStr(String element) {
    return element.replace("\n", "<br>");
  }

  @Override
  protected String handleMinecraftColor(AnyMinecraftColor element) {
    Map<String, Rgb> foreground =
        bedrock ? MotdColorTables.BEDROCK_FOREGROUND : MotdColorTables.JAVA_FOREGROUND;
    Map<String, Rgb> background =
        bedrock ? MotdColorTables.BEDROCK_BACKGROUND : MotdColorTables.JAVA_BACKGROUND;
    Rgb fg = foreground.get(element.name());
    Rgb bg = background.get(element.name());

    onReset.add("</span>");
    return "<span style='color:rgb("
        + fg.r()
        + ", "
        + fg.g()
        + ", "
        + fg.b()
        + ");text-shadow:0 0 1px rgb("
        + bg.r()
        + ", "
        + bg.g()
        + ", "
        + bg.b()
        + ")'>";
  }

  @Override
  protected String handleWebColor(WebColor element) {
    onReset.add("</span>");
    return "<span style='color:rgb("
        + element.r()
        + ", "
        + element.g()
        + ", "
        + element.b()
        + ")'>";
  }

  @Override
  protected String handleFormatting(AnyFormatting element) {
    if ("RESET".equals(element.name())) {
      String toReturn = String.join("", onReset);
      onReset = new ArrayList<>();
      return toReturn;
    }

    if ("OBFUSCATED".equals(element.name())) {
      onReset.add("</span>");
      return "<span class=obfuscated>";
    }

    String tagName = FORMATTING_TO_HTML_TAGS.get(element.name());
    onReset.add("</" + tagName + ">");
    return "<" + tagName + ">";
  }
}
