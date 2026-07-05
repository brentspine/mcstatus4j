package de.brentspine.mcstatus4j.responses;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Small helpers for reading required fields out of a loosely-typed JSON tree (as produced by
 * Jackson: nested {@code Map<String,Object>}/{@code List<Object>}/{@code String}/{@code Number}/
 * {@code Boolean}/{@code null}).
 *
 * <p>Mirrors the failure modes of Python mcstatus's direct {@code raw["key"]} dict indexing (a
 * missing key raises {@code KeyError}, a wrong-typed value eventually raises {@code TypeError}):
 * here a missing key raises {@link NoSuchElementException} and a wrong-typed value raises {@link
 * ClassCastException}.
 */
final class RawJson {

  private RawJson() {}

  static Object require(Map<String, Object> raw, String key) {
    if (!raw.containsKey(key)) {
      throw new NoSuchElementException("Missing required field '" + key + "'");
    }
    return raw.get(key);
  }

  static String requireString(Map<String, Object> raw, String key) {
    Object value = require(raw, key);
    if (!(value instanceof String s)) {
      throw new ClassCastException(
          "Field '" + key + "' must be a string, got " + describeType(value));
    }
    return s;
  }

  static int requireInt(Map<String, Object> raw, String key) {
    Object value = require(raw, key);
    if (!(value instanceof Number n)) {
      throw new ClassCastException(
          "Field '" + key + "' must be a number, got " + describeType(value));
    }
    return n.intValue();
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> asMap(Object value) {
    if (!(value instanceof Map<?, ?>)) {
      throw new ClassCastException("Expected a map, got " + describeType(value));
    }
    return (Map<String, Object>) value;
  }

  private static String describeType(Object value) {
    return value == null ? "null" : value.getClass().getName();
  }
}
