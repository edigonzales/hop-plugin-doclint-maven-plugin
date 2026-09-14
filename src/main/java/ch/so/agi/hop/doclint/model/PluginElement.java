package ch.so.agi.hop.doclint.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One annotated Hop plugin element discovered on the compiled classes. */
public final class PluginElement {

  private final PluginType type;
  private final String id;
  private final String rawId;
  private final String className;
  private final Map<String, Object> attributes;

  public PluginElement(PluginType type, String rawId, String className, Map<String, Object> attributes) {
    this.type = type;
    this.rawId = rawId == null ? "" : rawId;
    this.id = firstId(this.rawId);
    this.className = className;
    this.attributes = new LinkedHashMap<>(attributes);
  }

  private static String firstId(String rawId) {
    for (String token : rawId.split(",")) {
      if (!token.isBlank()) {
        return token.trim();
      }
    }
    return "";
  }

  public PluginType type() {
    return type;
  }

  /** The primary annotation id; aliases after the first comma are ignored. */
  public String id() {
    return id;
  }

  public String rawId() {
    return rawId;
  }

  public boolean hasIdAliases() {
    return rawId.contains(",");
  }

  public String className() {
    return className;
  }

  public boolean has(String attribute) {
    return !text(attribute).isBlank();
  }

  public String text(String attribute) {
    Object value = attributes.get(attribute);
    return value instanceof String text ? text.trim() : "";
  }

  public List<String> textList(String attribute) {
    return toTextList(attributes.get(attribute));
  }

  public boolean hasList(String attribute) {
    return !textList(attribute).isEmpty();
  }

  private static List<String> toTextList(Object value) {
    if (value == null) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    if (value instanceof Object[] array) {
      Arrays.stream(array).map(String::valueOf).forEach(values::add);
    } else if (value instanceof List<?> list) {
      list.stream().map(String::valueOf).forEach(values::add);
    } else {
      values.add(String.valueOf(value));
    }
    return values.stream().map(String::trim).filter(text -> !text.isBlank()).toList();
  }

  public String typeId() {
    return type.id();
  }

  @Override
  public String toString() {
    return type.label() + " " + id + " (" + className + ")";
  }
}
