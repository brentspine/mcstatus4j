package de.brentspine.mcstatus4j.motd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "ultimate" MOTD fixtures from Python mcstatus's {@code tests/motd/conftest.py}, covering
 * almost every formatting/color aspect in one shot.
 */
final class MotdFixtures {

  private MotdFixtures() {}

  static final String SOURCE_BEDROCK =
      "1" + "§0§k2" + "§1§l3" + "§2§o4" + "§35" + "§46" + "§57" + "§68" + "§79" + "§810" + "§911"
          + "§a12" + "§b13" + "§c14" + "§d15" + "§e16" + "§f17" + "§g18" + "§h19" + "§i20" + "§j21"
          + "§m22" + "§n23" + "§p24" + "§q25" + "§s26" + "§t27" + "§u28" + "§v29" + "§z30" + "§r31";

  private static Map<String, Object> m(Object... kv) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      map.put((String) kv[i], kv[i + 1]);
    }
    return map;
  }

  static Map<String, Object> sourceJava() {
    List<Object> extra =
        List.of(
            m("text", "1"),
            m("color", "#b3eeff", "text", "2"),
            m("obfuscated", true, "color", "black", "text", "3"),
            m("bold", true, "strikethrough", true, "color", "dark_blue", "text", "4"),
            m("italic", true, "color", "dark_green", "text", "5"),
            m("underlined", true, "color", "dark_aqua", "text", "6"),
            m("color", "dark_aqua", "text", "7"),
            m("color", "dark_red", "text", "8"),
            m("color", "dark_purple", "text", "9"),
            m("color", "gold", "text", "10"),
            m("color", "gray", "text", "11"),
            m("color", "dark_gray", "text", "12"),
            m("color", "blue", "text", "13"),
            m("color", "green", "text", "14"),
            m("color", "aqua", "text", "15"),
            m("color", "red", "text", "16"),
            m("color", "light_purple", "text", "17"),
            m("color", "yellow", "text", "18"),
            m("color", "white", "text", "19"),
            m("color", "reset", "text", "20"),
            m("translate", "some.random.string"));
    return m("extra", extra, "text", "top");
  }
}
