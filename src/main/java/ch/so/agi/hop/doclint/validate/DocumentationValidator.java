package ch.so.agi.hop.doclint.validate;

import ch.so.agi.hop.doclint.config.DocLintConfig;
import ch.so.agi.hop.doclint.doc.AsciiDocPage;
import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.PluginElement;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates the AsciiDoc page of every discovered plugin element.
 *
 * <p>The page is derived from {@code documentationUrl} by convention: the URL file name
 * ({@code .../transforms/example.html}) maps to {@code docs/transforms/example.adoc}. No mapping
 * file exists. The annotation id and the {@code :plugin-id:} attribute must match.
 */
public final class DocumentationValidator {

  public List<CheckResult> validate(List<PluginElement> elements, DocLintConfig config)
      throws IOException {
    List<CheckResult> results = new ArrayList<>();
    for (PluginElement element : elements) {
      if (!config.requireDocumentation()) {
        results.add(CheckResult.skip(element.id(), "documentation checks are disabled"));
        continue;
      }
      String url = element.text("documentationUrl");
      if (url.isBlank()) {
        results.add(CheckResult.fail(element.id(), "documentationUrl is missing"));
        continue;
      }
      results.add(CheckResult.ok(element.id(), "documentationUrl", url));

      String pageName = pageName(url);
      if (pageName == null) {
        results.add(
            CheckResult.fail(element.id(), "documentationUrl does not point to a page", url));
        continue;
      }
      Path page =
          config
              .docsDirectory()
              .resolve(element.type().docsSubdirectory())
              .resolve(pageName + ".adoc");
      if (!Files.isRegularFile(page)) {
        results.add(
            CheckResult.fail(element.id(), "documentation page is missing", config.display(page)));
        continue;
      }
      results.add(CheckResult.ok(element.id(), "documentation page", config.display(page)));
      results.addAll(checkPage(element, config, page));
    }
    return results;
  }

  private List<CheckResult> checkPage(
      PluginElement element, DocLintConfig config, Path page) throws IOException {
    List<CheckResult> results = new ArrayList<>();
    String id = element.id();
    AsciiDocPage doc = AsciiDocPage.read(page);
    results.add(checkAttribute(id, doc, "plugin-id", id));
    results.add(checkAttribute(id, doc, "plugin-type", element.typeId()));
    results.add(checkAttributePresent(id, doc, "description"));
    for (String section : element.type().requiredSections()) {
      if (!doc.hasSection(section)) {
        results.add(
            CheckResult.fail(
                id,
                "required section '" + section + "' is missing in " + config.display(page)));
      } else if (!doc.hasSectionContent(section)) {
        results.add(
            CheckResult.fail(
                id,
                "required section '" + section + "' is empty in " + config.display(page)));
      } else {
        results.add(CheckResult.ok(id, "section: " + section));
      }
    }
    return results;
  }

  private CheckResult checkAttribute(
      String id, AsciiDocPage doc, String attribute, String expected) {
    String actual = doc.attribute(attribute).orElse("");
    if (actual.isBlank()) {
      return CheckResult.fail(id, ":" + attribute + ": is missing");
    }
    if (!expected.equals(actual)) {
      return CheckResult.fail(
          id, ":" + attribute + ": is '" + actual + "' but expected '" + expected + "'");
    }
    return CheckResult.ok(id, ":" + attribute + ":", actual);
  }

  private CheckResult checkAttributePresent(String id, AsciiDocPage doc, String attribute) {
    String actual = doc.attribute(attribute).orElse("");
    if (actual.isBlank()) {
      return CheckResult.fail(id, ":" + attribute + ": is missing");
    }
    return CheckResult.ok(id, ":" + attribute + ":", actual);
  }

  /** Extracts the local page name from a documentation URL, or {@code null} if it has none. */
  static String pageName(String url) {
    String path;
    try {
      path = URI.create(url).getPath();
    } catch (IllegalArgumentException e) {
      return null;
    }
    if (path == null || path.isBlank()) {
      return null;
    }
    String name = path.substring(path.lastIndexOf('/') + 1);
    if (name.isBlank()) {
      return null;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".html")) {
      name = name.substring(0, name.length() - ".html".length());
    } else if (lower.endsWith(".htm")) {
      name = name.substring(0, name.length() - ".htm".length());
    }
    return name.isBlank() ? null : name;
  }
}
