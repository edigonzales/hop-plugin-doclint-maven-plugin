package ch.so.agi.hop.doclint.validate;

import ch.so.agi.hop.doclint.config.DocLintConfig;
import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.PluginElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Validates the Hop annotation metadata of every discovered plugin element. */
public final class MetadataValidator {

  public List<CheckResult> validate(
      List<PluginElement> elements, DocLintConfig config, Path classesDirectory) {
    Path classesRoot = classesDirectory.toAbsolutePath().normalize();
    List<CheckResult> results = new ArrayList<>();
    for (PluginElement element : elements) {
      String id = element.id();
      results.add(required(element, "id", "id"));
      if (element.hasIdAliases()) {
        results.add(
            CheckResult.warn(
                id,
                "id contains aliases",
                element.rawId() + " (only the first id is used for documentation checks)"));
      }
      results.add(required(element, "name", "name"));
      results.add(required(element, "description", "description"));
      results.add(required(element, "image", "image"));
      results.add(required(element, "categoryDescription", "categoryDescription"));
      results.add(checkKeywords(element, config));
      if (element.has("image")) {
        results.add(checkImageResource(element, id, classesRoot));
      }
      results.add(checkEngineDeclarations(element, id));
    }
    return results;
  }

  private CheckResult required(PluginElement element, String attribute, String label) {
    if (!element.has(attribute)) {
      return CheckResult.fail(element.id(), label + " is missing");
    }
    return CheckResult.ok(element.id(), label, element.text(attribute));
  }

  private CheckResult checkKeywords(PluginElement element, DocLintConfig config) {
    List<String> keywords = element.textList("keywords");
    if (!keywords.isEmpty()) {
      return CheckResult.ok(element.id(), "keywords", String.join(", ", keywords));
    }
    if (config.requireKeywords()) {
      return CheckResult.fail(element.id(), "keywords are missing");
    }
    return CheckResult.skip(element.id(), "keywords (not required)");
  }

  private CheckResult checkImageResource(
      PluginElement element, String id, Path classesRoot) {
    String image = element.text("image").replace('\\', '/');
    while (image.startsWith("/")) {
      image = image.substring(1);
    }
    Path resource = classesRoot.resolve(image).normalize();
    if (resource.startsWith(classesRoot) && Files.isRegularFile(resource)) {
      return CheckResult.ok(id, "image resource", element.text("image"));
    }
    return CheckResult.fail(id, "image resource is missing from the compiled classes", image);
  }

  private CheckResult checkEngineDeclarations(PluginElement element, String id) {
    List<String> supported = element.textList("supportedEngines");
    List<String> excluded = element.textList("excludedEngines");
    if (!supported.isEmpty() && !excluded.isEmpty()) {
      return CheckResult.fail(
          id, "supportedEngines and excludedEngines must not be set together");
    }
    if (!supported.isEmpty()) {
      return CheckResult.ok(id, "supportedEngines", String.join(", ", supported));
    }
    if (!excluded.isEmpty()) {
      return CheckResult.ok(id, "excludedEngines", String.join(", ", excluded));
    }
    return CheckResult.ok(id, "supportedEngines/excludedEngines", "none");
  }
}
