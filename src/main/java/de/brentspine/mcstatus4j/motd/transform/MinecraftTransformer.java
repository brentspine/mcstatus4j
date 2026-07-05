package de.brentspine.mcstatus4j.motd.transform;

import de.brentspine.mcstatus4j.motd.AnyFormatting;
import de.brentspine.mcstatus4j.motd.AnyMinecraftColor;
import de.brentspine.mcstatus4j.motd.InvalidFormatting;
import de.brentspine.mcstatus4j.motd.MotdComponent;
import java.util.List;

/**
 * Renders a MOTD back into Minecraft's own {@code §}-code format. Backs {@code Motd.toMinecraft()}.
 *
 * <p>Always emits {@code §}, even if the original MOTD used {@code &}.
 */
public final class MinecraftTransformer extends PlainTransformer {

  public MinecraftTransformer(boolean bedrock) {
    super(bedrock);
  }

  @Override
  protected List<String> handleComponent(MotdComponent component) {
    // A color code inherently resets prior formatting in Minecraft's own representation, so the
    // base class's auto-injected reset-before-color pair collapses down to just the color code.
    List<String> result = super.handleComponent(component);
    if (result.size() == 2) {
      return List.of(result.get(1));
    }
    return result;
  }

  @Override
  protected String handleMinecraftColor(AnyMinecraftColor element) {
    return "§" + element.code();
  }

  @Override
  protected String handleFormatting(AnyFormatting element) {
    return "§" + element.code();
  }

  @Override
  protected String handleInvalidFormatting(InvalidFormatting element) {
    return "§" + element.value();
  }
}
