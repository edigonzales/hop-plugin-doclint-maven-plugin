package ch.so.agi.hop.doclint.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.hop.doclint.model.PluginElement;
import ch.so.agi.hop.doclint.scan.PluginElementScanner.ScanOutcome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginElementScannerTest {

  private final PluginElementScanner scanner = new PluginElementScanner();

  @Test
  void findsAnnotatedTransformsAndReadsAttributes() throws Exception {
    List<PluginElement> elements = scanner.scan(Path.of("target/test-classes")).elements();

    PluginElement full = element(elements, "FULL_TRANSFORM");
    assertEquals("Full Transform", full.text("name"));
    assertEquals("Fixture with all attributes set", full.text("description"));
    assertEquals(List.of("fixture", "documentation"), full.textList("keywords"));
    assertEquals(List.of("Local"), full.textList("supportedEngines"));
    assertEquals(
        "https://example.org/hop/transforms/full-transform.html", full.text("documentationUrl"));
    assertEquals("ch.so.agi.hop.doclint.fixtures.FullTransformMeta", full.className());
    assertEquals("transform", full.typeId());
  }

  @Test
  void readsMinimalTransformWithoutOptionalAttributes() throws Exception {
    List<PluginElement> elements = scanner.scan(Path.of("target/test-classes")).elements();

    PluginElement minimal = element(elements, "MINIMAL_TRANSFORM");
    assertEquals("Minimal Transform", minimal.text("name"));
    assertTrue(minimal.textList("keywords").isEmpty());
    assertTrue(minimal.text("documentationUrl").isEmpty());
  }

  @Test
  void usesFirstIdOfAliases() throws Exception {
    List<PluginElement> elements = scanner.scan(Path.of("target/test-classes")).elements();

    PluginElement alias = element(elements, "ALIAS_TRANSFORM");
    assertTrue(alias.hasIdAliases());
    assertEquals("ALIAS_TRANSFORM,OLD_ALIAS_TRANSFORM", alias.rawId());
  }

  @Test
  void findsNothingInDirectoryWithoutCompiledClasses(@TempDir Path directory) throws Exception {
    ScanOutcome outcome = scanner.scan(directory);

    assertTrue(outcome.elements().isEmpty());
    assertEquals(0, outcome.scannedClasses());
  }

  @Test
  void scansOnlyTheGivenDirectory(@TempDir Path directory) throws IOException {
    Path fixture =
        Path.of(
            "target/test-classes/ch/so/agi/hop/doclint/fixtures/FullTransformMeta.class");
    Path target = directory.resolve("ch/so/agi/hop/doclint/fixtures/FullTransformMeta.class");
    Files.createDirectories(target.getParent());
    Files.copy(fixture, target);

    ScanOutcome outcome = scanner.scan(directory);

    assertEquals(1, outcome.scannedClasses());
    assertEquals(1, outcome.elements().size());
    assertEquals("FULL_TRANSFORM", outcome.elements().get(0).id());
  }

  private PluginElement element(List<PluginElement> elements, String id) {
    return elements.stream()
        .filter(element -> id.equals(element.id()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing fixture element " + id));
  }
}
