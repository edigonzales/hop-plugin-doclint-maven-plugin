package ch.so.agi.hop.doclint.validate;

import static ch.so.agi.hop.doclint.TestElements.contains;
import static ch.so.agi.hop.doclint.TestElements.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.hop.doclint.config.DocLintConfig;
import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.CheckStatus;
import ch.so.agi.hop.doclint.model.PluginElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationValidatorTest {

  private static final String DOCUMENTATION_URL =
      "https://example.org/hop/transforms/example-transform.html";

  @TempDir Path temp;

  private Path docs;
  private final DocumentationValidator validator = new DocumentationValidator();

  @BeforeEach
  void setUp() throws IOException {
    docs = Files.createDirectories(temp.resolve("docs/transforms"));
  }

  private DocLintConfig config(boolean requireDocumentation) {
    return new DocLintConfig(
        temp, temp.resolve("docs"), temp.resolve("examples"), true, requireDocumentation, true);
  }

  private PluginElement element(String documentationUrl) {
    return transform(
        Map.of(
            "id", "EXAMPLE_TRANSFORM",
            "name", "Example Transform",
            "documentationUrl", documentationUrl));
  }

  private void writePage(String pluginId, String optionsSection) throws IOException {
    Files.writeString(
        docs.resolve("example-transform.adoc"),
        """
        = Example Transform
        :plugin-id: %s
        :plugin-type: transform
        :description: Example description

        == Description
        Reads rows.
        %s
        == Input
        One incoming row per file.

        == Output
        One outgoing row per input row.

        == Supported engines
        Local.

        == Examples
        See examples/.

        == Error handling
        Fails on invalid input.

        == Limitations
        None.
        """
            .formatted(pluginId, optionsSection));
  }

  private List<CheckResult> validate(PluginElement element, boolean require) throws IOException {
    return validator.validate(List.of(element), config(require));
  }

  @Test
  void validatesAMatchingDocumentationPage() throws IOException {
    writePage("EXAMPLE_TRANSFORM", "\n== Options\n\n|===\n|Option |Description\n|===\n");

    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.OK, ":plugin-id:"));
    assertTrue(contains(results, CheckStatus.OK, "section: Options"));
  }

  @Test
  void failsWhenDocumentationUrlIsMissing() throws IOException {
    List<CheckResult> results = validate(element(""), true);

    assertTrue(contains(results, CheckStatus.FAIL, "documentationUrl is missing"));
  }

  @Test
  void failsWhenTheDocumentationPageIsMissing() throws IOException {
    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertTrue(contains(results, CheckStatus.FAIL, "documentation page is missing"));
  }

  @Test
  void failsWhenPluginIdDoesNotMatch() throws IOException {
    writePage("OTHER_TRANSFORM", "\n== Options\n\nSome option.\n");

    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertTrue(contains(results, CheckStatus.FAIL, ":plugin-id:"));
  }

  @Test
  void failsWhenAPluginTypeDoesNotMatch() throws IOException {
    Files.writeString(
        docs.resolve("example-transform.adoc"),
        """
        = Example Transform
        :plugin-id: EXAMPLE_TRANSFORM
        :plugin-type: action
        :description: Example description

        == Description
        Reads rows.

        == Input
        One row.

        == Options
        Some option.

        == Output
        One row.

        == Supported engines
        Local.

        == Examples
        See examples/.

        == Error handling
        Fails.

        == Limitations
        None.
        """);

    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertTrue(contains(results, CheckStatus.FAIL, ":plugin-type:"));
  }

  @Test
  void failsWhenARequiredSectionIsEmpty() throws IOException {
    writePage("EXAMPLE_TRANSFORM", "\n== Options\n\n// TODO\n");

    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertTrue(contains(results, CheckStatus.FAIL, "section 'Options' is empty"));
  }

  @Test
  void failsWhenARequiredSectionIsMissing() throws IOException {
    writePage("EXAMPLE_TRANSFORM", "");

    List<CheckResult> results = validate(element(DOCUMENTATION_URL), true);

    assertTrue(contains(results, CheckStatus.FAIL, "section 'Options' is missing"));
  }

  @Test
  void skipsEveryCheckWhenDocumentationIsNotRequired() throws IOException {
    List<CheckResult> results = validate(element(""), false);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.SKIP, "documentation checks are disabled"));
  }

  @Test
  void derivesThePageNameFromTheUrl() {
    assertEquals("example-transform", DocumentationValidator.pageName(DOCUMENTATION_URL));
    assertEquals("plain", DocumentationValidator.pageName("https://example.org/plain.htm"));
    assertEquals("page", DocumentationValidator.pageName("https://example.org/sub/page"));
    assertNull(DocumentationValidator.pageName("https://example.org/"));
    assertNull(DocumentationValidator.pageName("not a url"));
  }
}
