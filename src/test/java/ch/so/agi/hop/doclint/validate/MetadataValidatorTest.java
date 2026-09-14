package ch.so.agi.hop.doclint.validate;

import static ch.so.agi.hop.doclint.TestElements.contains;
import static ch.so.agi.hop.doclint.TestElements.transform;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class MetadataValidatorTest {

  @TempDir Path temp;

  private Path classes;
  private final MetadataValidator validator = new MetadataValidator();

  @BeforeEach
  void setUp() throws IOException {
    classes = Files.createDirectories(temp.resolve("classes"));
    Path icon = classes.resolve("example/icon.svg");
    Files.createDirectories(icon.getParent());
    Files.writeString(icon, "<svg/>");
  }

  private DocLintConfig config(boolean requireKeywords) {
    return new DocLintConfig(
        temp, temp.resolve("docs"), temp.resolve("examples"), requireKeywords, true, true);
  }

  private PluginElement validTransform() {
    return transform(
        Map.of(
            "id", "EXAMPLE_TRANSFORM",
            "name", "Example Transform",
            "description", "Example description",
            "image", "/example/icon.svg",
            "categoryDescription", "Examples",
            "keywords", new Object[] {"example", "documentation"}));
  }

  private List<CheckResult> validate(PluginElement element, boolean requireKeywords) {
    return validator.validate(List.of(element), config(requireKeywords), classes);
  }

  @Test
  void acceptsCompleteMetadata() {
    List<CheckResult> results = validate(validTransform(), true);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.OK, "image resource"));
    assertTrue(contains(results, CheckStatus.OK, "keywords"));
  }

  @Test
  void failsOnMissingRequiredAttributes() {
    List<CheckResult> results = validate(transform(Map.of("id", "EXAMPLE_TRANSFORM", "name", "X")), true);

    assertTrue(contains(results, CheckStatus.FAIL, "description is missing"));
    assertTrue(contains(results, CheckStatus.FAIL, "image is missing"));
    assertTrue(contains(results, CheckStatus.FAIL, "categoryDescription is missing"));
    assertTrue(contains(results, CheckStatus.FAIL, "keywords are missing"));
  }

  @Test
  void failsWhenIconIsNotInTheCompiledClasses() {
    PluginElement element =
        transform(
            Map.of(
                "id", "EXAMPLE_TRANSFORM",
                "name", "Example Transform",
                "description", "Example description",
                "image", "example/missing.svg",
                "categoryDescription", "Examples",
                "keywords", new Object[] {"example"}));

    List<CheckResult> results = validate(element, true);

    assertTrue(contains(results, CheckStatus.FAIL, "image resource is missing"));
  }

  @Test
  void acceptsMissingKeywordsWhenTheyAreNotRequired() {
    PluginElement withoutKeywords =
        transform(
            Map.of(
                "id", "EXAMPLE_TRANSFORM",
                "name", "Example Transform",
                "description", "Example description",
                "image", "example/icon.svg",
                "categoryDescription", "Examples"));

    List<CheckResult> results = validate(withoutKeywords, false);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.SKIP, "keywords"));
  }

  @Test
  void failsWhenSupportedAndExcludedEnginesAreBothSet() {
    PluginElement element =
        transform(
            Map.of(
                "id", "EXAMPLE_TRANSFORM",
                "name", "Example Transform",
                "description", "Example description",
                "image", "example/icon.svg",
                "categoryDescription", "Examples",
                "keywords", new Object[] {"example"},
                "supportedEngines", new Object[] {"Local"},
                "excludedEngines", new Object[] {"BeamDirectPipelineEngine"}));

    List<CheckResult> results = validate(element, true);

    assertTrue(contains(results, CheckStatus.FAIL, "must not be set together"));
  }

  @Test
  void warnsAboutAliasIds() {
    List<CheckResult> results = validate(transform(Map.of("id", "EXAMPLE_TRANSFORM,OLD")), true);

    assertTrue(contains(results, CheckStatus.WARN, "id contains aliases"));
  }
}
