package de.brentspine.mcstatus4j.motd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Ported from Python mcstatus's {@code tests/motd/test_transformers.py}. */
class MotdTransformersTest {

  @Test
  void plainText() {
    assertEquals("plain", Motd.parse("plain", false).toPlain());
  }

  @Test
  void plainRemovesColors() {
    assertEquals("text", Motd.parse("&1&ltext", false).toPlain());
  }

  @Test
  void plainSkipsWebColors() {
    Map<String, Object> input =
        Map.of(
            "extra",
            java.util.List.of(Map.of("color", "#4000ff", "text", "colored text")),
            "text",
            "");
    assertEquals("colored text", Motd.parse(input, false).toPlain());
  }

  @Test
  void plainSkipsMinecraftColors() {
    Map<String, Object> input =
        Map.of(
            "extra", java.util.List.of(Map.of("color", "red", "text", "colored text")), "text", "");
    assertEquals("colored text", Motd.parse(input, false).toPlain());
  }

  @ParameterizedTest
  @ValueSource(strings = {"&1&2&3&z", "§123§5bc&z", "§1§2§3§z"})
  void minecraftReturnsSameWithSectionSign(String motd) {
    assertEquals(motd.replace('&', '§'), Motd.parse(motd, false).toMinecraft());
  }

  @Test
  void minecraftSkipsWebColorsButEmitsResetPrefix() {
    Map<String, Object> input =
        Map.of(
            "extra",
            java.util.List.of(Map.of("color", "#4000ff", "text", "colored text")),
            "text",
            "");
    assertEquals("§rcolored text§r", Motd.parse(input, false).toMinecraft());
  }

  @Test
  void htmlCorrectOutputJava() {
    String expected =
        "<p>top"
            + "1<span style='color:rgb(179, 238, 255)'>2</span>"
            + "<span style='color:rgb(0, 0, 0);text-shadow:0 0 1px rgb(0, 0, 0)'><span class=obfuscated>3</span>"
            + "</span>"
            + "<span style='color:rgb(0, 0, 170);text-shadow:0 0 1px rgb(0, 0, 42)'><b><s>4</span></b></s>"
            + "<span style='color:rgb(0, 170, 0);text-shadow:0 0 1px rgb(0, 42, 0)'><i>5</span></i>"
            + "<span style='color:rgb(0, 170, 170);text-shadow:0 0 1px rgb(0, 42, 42)'><u>6</span></u>"
            + "<span style='color:rgb(0, 170, 170);text-shadow:0 0 1px rgb(0, 42, 42)'>7</span>"
            + "<span style='color:rgb(170, 0, 0);text-shadow:0 0 1px rgb(42, 0, 0)'>8</span>"
            + "<span style='color:rgb(170, 0, 170);text-shadow:0 0 1px rgb(42, 0, 42)'>9</span>"
            + "<span style='color:rgb(255, 170, 0);text-shadow:0 0 1px rgb(64, 42, 0)'>10</span>"
            + "<span style='color:rgb(170, 170, 170);text-shadow:0 0 1px rgb(42, 42, 42)'>11</span>"
            + "<span style='color:rgb(85, 85, 85);text-shadow:0 0 1px rgb(21, 21, 21)'>12</span>"
            + "<span style='color:rgb(85, 85, 255);text-shadow:0 0 1px rgb(21, 21, 63)'>13</span>"
            + "<span style='color:rgb(85, 255, 85);text-shadow:0 0 1px rgb(21, 63, 21)'>14</span>"
            + "<span style='color:rgb(85, 255, 255);text-shadow:0 0 1px rgb(21, 63, 63)'>15</span>"
            + "<span style='color:rgb(255, 85, 85);text-shadow:0 0 1px rgb(63, 21, 21)'>16</span>"
            + "<span style='color:rgb(255, 85, 255);text-shadow:0 0 1px rgb(63, 21, 63)'>17</span>"
            + "<span style='color:rgb(255, 255, 85);text-shadow:0 0 1px rgb(63, 63, 21)'>18</span>"
            + "<span style='color:rgb(255, 255, 255);text-shadow:0 0 1px rgb(63, 63, 63)'>19</span>"
            + "20</p>";
    assertEquals(expected, Motd.parse(MotdFixtures.sourceJava(), false).toHtml());
  }

  @Test
  void htmlCorrectOutputBedrock() {
    String expected =
        "<p>"
            + "1"
            + "<span style='color:rgb(0, 0, 0);text-shadow:0 0 1px rgb(0, 0, 0)'><span class=obfuscated>2</span></span>"
            + "<span style='color:rgb(0, 0, 170);text-shadow:0 0 1px rgb(0, 0, 42)'><b>3</span></b>"
            + "<span style='color:rgb(0, 170, 0);text-shadow:0 0 1px rgb(0, 42, 0)'><i>4</span></i>"
            + "<span style='color:rgb(0, 170, 170);text-shadow:0 0 1px rgb(0, 42, 42)'>5</span>"
            + "<span style='color:rgb(170, 0, 0);text-shadow:0 0 1px rgb(42, 0, 0)'>6</span>"
            + "<span style='color:rgb(170, 0, 170);text-shadow:0 0 1px rgb(42, 0, 42)'>7</span>"
            + "<span style='color:rgb(255, 170, 0);text-shadow:0 0 1px rgb(64, 42, 0)'>8</span>"
            + "<span style='color:rgb(198, 198, 198);text-shadow:0 0 1px rgb(49, 49, 49)'>9</span>"
            + "<span style='color:rgb(85, 85, 85);text-shadow:0 0 1px rgb(21, 21, 21)'>10</span>"
            + "<span style='color:rgb(85, 85, 255);text-shadow:0 0 1px rgb(21, 21, 63)'>11</span>"
            + "<span style='color:rgb(85, 255, 85);text-shadow:0 0 1px rgb(21, 63, 21)'>12</span>"
            + "<span style='color:rgb(85, 255, 255);text-shadow:0 0 1px rgb(21, 63, 63)'>13</span>"
            + "<span style='color:rgb(255, 85, 85);text-shadow:0 0 1px rgb(63, 21, 21)'>14</span>"
            + "<span style='color:rgb(255, 85, 255);text-shadow:0 0 1px rgb(63, 21, 63)'>15</span>"
            + "<span style='color:rgb(255, 255, 85);text-shadow:0 0 1px rgb(63, 63, 21)'>16</span>"
            + "<span style='color:rgb(255, 255, 255);text-shadow:0 0 1px rgb(63, 63, 63)'>17</span>"
            + "<span style='color:rgb(221, 214, 5);text-shadow:0 0 1px rgb(55, 53, 1)'>18</span>"
            + "<span style='color:rgb(227, 212, 209);text-shadow:0 0 1px rgb(56, 53, 52)'>19</span>"
            + "<span style='color:rgb(206, 202, 202);text-shadow:0 0 1px rgb(51, 50, 50)'>20</span>"
            + "<span style='color:rgb(68, 58, 59);text-shadow:0 0 1px rgb(17, 14, 14)'>21</span>"
            + "<span style='color:rgb(151, 22, 7);text-shadow:0 0 1px rgb(37, 5, 1)'>22</span>"
            + "<span style='color:rgb(180, 104, 77);text-shadow:0 0 1px rgb(45, 26, 19)'>23</span>"
            + "<span style='color:rgb(222, 177, 45);text-shadow:0 0 1px rgb(55, 44, 11)'>24</span>"
            + "<span style='color:rgb(17, 159, 54);text-shadow:0 0 1px rgb(4, 40, 13)'>25</span>"
            + "<span style='color:rgb(44, 186, 168);text-shadow:0 0 1px rgb(11, 46, 42)'>26</span>"
            + "<span style='color:rgb(33, 73, 123);text-shadow:0 0 1px rgb(8, 18, 30)'>27</span>"
            + "<span style='color:rgb(154, 92, 198);text-shadow:0 0 1px rgb(38, 23, 49)'>28</span>"
            + "<span style='color:rgb(235, 114, 20);text-shadow:0 0 1px rgb(59, 29, 5)'>2930</span>"
            + "31"
            + "</p>";
    assertEquals(expected, Motd.parse(MotdFixtures.SOURCE_BEDROCK, true).toHtml());
  }

  @Test
  void htmlNewLineIsBrTag() {
    Motd motd = Motd.parse("Some cool\ntext", false);
    assertEquals("<p>Some cool<br>text</p>", motd.toHtml());
  }

  @Test
  void ansiCorrectOutputJava() {
    String expected =
        "\033[0mtop\033[0m"
            + "1\033[0m"
            + "\033[38;2;179;238;255m2\033[0m\033[0m"
            + "\033[38;2;0;0;0m\033[5m3\033[0m\033[0m"
            + "\033[38;2;0;0;170m\033[1m\033[9m4\033[0m\033[0m"
            + "\033[38;2;0;170;0m\033[3m5\033[0m\033[0m"
            + "\033[38;2;0;170;170m\033[4m6\033[0m\033[0m"
            + "\033[38;2;0;170;170m7\033[0m\033[0m"
            + "\033[38;2;170;0;0m8\033[0m\033[0m"
            + "\033[38;2;170;0;170m9\033[0m\033[0m"
            + "\033[38;2;255;170;0m10\033[0m\033[0m"
            + "\033[38;2;170;170;170m11\033[0m\033[0m"
            + "\033[38;2;85;85;85m12\033[0m\033[0m"
            + "\033[38;2;85;85;255m13\033[0m\033[0m"
            + "\033[38;2;85;255;85m14\033[0m\033[0m"
            + "\033[38;2;85;255;255m15\033[0m\033[0m"
            + "\033[38;2;255;85;85m16\033[0m\033[0m"
            + "\033[38;2;255;85;255m17\033[0m\033[0m"
            + "\033[38;2;255;255;85m18\033[0m\033[0m"
            + "\033[38;2;255;255;255m19\033[0m\033[0m"
            + "20\033[0m"
            + "\033[0m\033[0m";
    assertEquals(expected, Motd.parse(MotdFixtures.sourceJava(), false).toAnsi());
  }

  @Test
  void ansiCorrectOutputBedrock() {
    String expected =
        "\033[0m"
            + "1\033[0m"
            + "\033[38;2;0;0;0m\033[5m2"
            + "\033[0m\033[38;2;0;0;170m\033[1m3"
            + "\033[0m\033[38;2;0;170;0m\033[3m4"
            + "\033[0m\033[38;2;0;170;170m5"
            + "\033[0m\033[38;2;170;0;0m6"
            + "\033[0m\033[38;2;170;0;170m7"
            + "\033[0m\033[38;2;255;170;0m8"
            + "\033[0m\033[38;2;198;198;198m9"
            + "\033[0m\033[38;2;85;85;85m10"
            + "\033[0m\033[38;2;85;85;255m11"
            + "\033[0m\033[38;2;85;255;85m12"
            + "\033[0m\033[38;2;85;255;255m13"
            + "\033[0m\033[38;2;255;85;85m14"
            + "\033[0m\033[38;2;255;85;255m15"
            + "\033[0m\033[38;2;255;255;85m16"
            + "\033[0m\033[38;2;255;255;255m17"
            + "\033[0m\033[38;2;221;214;5m18"
            + "\033[0m\033[38;2;227;212;209m19"
            + "\033[0m\033[38;2;206;202;202m20"
            + "\033[0m\033[38;2;68;58;59m21"
            + "\033[0m\033[38;2;151;22;7m22"
            + "\033[0m\033[38;2;180;104;77m23"
            + "\033[0m\033[38;2;222;177;45m24"
            + "\033[0m\033[38;2;17;159;54m25"
            + "\033[0m\033[38;2;44;186;168m26"
            + "\033[0m\033[38;2;33;73;123m27"
            + "\033[0m\033[38;2;154;92;198m28"
            + "\033[0m\033[38;2;235;114;20m29"
            + "30\033[0m31\033[0m";
    assertEquals(expected, Motd.parse(MotdFixtures.SOURCE_BEDROCK, true).toAnsi());
  }
}
