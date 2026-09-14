package ch.so.agi.hop.doclint;

import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.CheckStatus;
import ch.so.agi.hop.doclint.model.PluginElement;
import ch.so.agi.hop.doclint.model.PluginType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small helpers shared by the validator tests. */
public final class TestElements {

  private TestElements() {}

  public static PluginElement transform(Map<String, Object> attributes) {
    Map<String, Object> values = new LinkedHashMap<>(attributes);
    Object id = values.getOrDefault("id", "");
    return new PluginElement(
        PluginType.TRANSFORM, String.valueOf(id), "example.TestTransformMeta", values);
  }

  public static boolean contains(List<CheckResult> results, CheckStatus status, String part) {
    return results.stream()
        .anyMatch(
            result ->
                result.status() == status
                    && ((result.check() != null && result.check().contains(part))
                        || (result.detail() != null && result.detail().contains(part))));
  }
}
