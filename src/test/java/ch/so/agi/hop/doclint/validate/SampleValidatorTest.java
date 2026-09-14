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

class SampleValidatorTest {

  @TempDir Path temp;

  private Path examples;
  private final SampleValidator validator = new SampleValidator();

  @BeforeEach
  void setUp() throws IOException {
    examples = Files.createDirectories(temp.resolve("examples/demo"));
  }

  private DocLintConfig config(boolean requireExamples) {
    return new DocLintConfig(
        temp, temp.resolve("docs"), temp.resolve("examples"), true, true, requireExamples);
  }

  private PluginElement element() {
    return transform(Map.of("id", "EXAMPLE_TRANSFORM", "name", "Example Transform"));
  }

  private List<CheckResult> validate(PluginElement element, boolean requireExamples)
      throws IOException {
    return validator.validate(List.of(element), config(requireExamples));
  }

  @Test
  void acceptsAnExampleUsingThePluginId() throws IOException {
    Files.writeString(
        examples.resolve("example.hpl"),
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <pipeline>
          <transform>
            <type>EXAMPLE_TRANSFORM</type>
            <name>Example</name>
          </transform>
        </pipeline>
        """);

    List<CheckResult> results = validate(element(), true);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.OK, "examples/demo/example.hpl"));
  }

  @Test
  void failsWhenNoExampleUsesThePluginId() throws IOException {
    Files.writeString(
        examples.resolve("example.hpl"),
        "<pipeline><transform><type>OTHER_TRANSFORM</type></transform></pipeline>");

    List<CheckResult> results = validate(element(), true);

    assertTrue(contains(results, CheckStatus.FAIL, "no example uses EXAMPLE_TRANSFORM"));
  }

  @Test
  void failsWhenTheExamplesDirectoryIsMissing() throws IOException {
    List<CheckResult> results =
        validator.validate(
            List.of(element()),
            new DocLintConfig(
                temp, temp.resolve("docs"), temp.resolve("missing"), true, true, true));

    assertTrue(contains(results, CheckStatus.FAIL, "examples directory is missing"));
  }

  @Test
  void reportsUnreadableExamples() throws IOException {
    Files.writeString(examples.resolve("broken.hpl"), "<pipeline><transform>");

    List<CheckResult> results = validate(element(), true);

    assertTrue(contains(results, CheckStatus.FAIL, "example pipeline is not readable"));
  }

  @Test
  void skipsTheCheckWhenExamplesAreNotRequired() throws IOException {
    List<CheckResult> results = validate(element(), false);

    assertFalse(results.stream().anyMatch(CheckResult::isError));
    assertTrue(contains(results, CheckStatus.SKIP, "example (not required)"));
  }
}
