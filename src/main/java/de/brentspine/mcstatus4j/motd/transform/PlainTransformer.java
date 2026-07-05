package de.brentspine.mcstatus4j.motd.transform;

import java.util.List;

/** Renders a MOTD as plain text, dropping all colors/formatting. Backs {@code Motd.toPlain()}. */
public class PlainTransformer extends MotdTransformer {

  public PlainTransformer(boolean bedrock) {
    super(bedrock);
  }

  @Override
  protected String formatOutput(List<String> results) {
    return String.join("", results);
  }

  @Override
  protected String handleStr(String element) {
    return element;
  }
}
