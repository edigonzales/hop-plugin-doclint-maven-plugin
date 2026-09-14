package ch.so.agi.hop.doclint.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AsciiDocPageTest {

  @Test
  void readsDocumentAttributes() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            = Example Transform
            :plugin-id: EXAMPLE_TRANSFORM
            :plugin-type: transform
            :description: Reads rows.

            == Description

            Some text.
            """);

    assertEquals("EXAMPLE_TRANSFORM", page.attribute("plugin-id").orElseThrow());
    assertEquals("transform", page.attribute("plugin-type").orElseThrow());
    assertEquals("Reads rows.", page.attribute("description").orElseThrow());
    assertTrue(page.attribute("missing").isEmpty());
  }

  @Test
  void detectsSectionsAndContent() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            == Description

            Some text.

            == Limitations

            // only a comment

            == Options

            |===
            |Option |Description
            |===
            """);

    assertTrue(page.hasSection("description"));
    assertTrue(page.hasSectionContent("description"));
    assertTrue(page.hasSection("limitations"));
    assertFalse(page.hasSectionContent("limitations"));
    assertTrue(page.hasSectionContent("options"));
    assertFalse(page.hasSection("missing"));
  }

  @Test
  void treatsSubsectionsAsContent() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            == Options

            === General

            Some option.
            """);

    assertTrue(page.hasSectionContent("options"));
  }

  @Test
  void keepsFirstOccurrenceOfDuplicateSections() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            == Description

            first

            == Description

            second
            """);

    assertTrue(page.hasSection("description"));
  }

  @Test
  void ignoresAttributeLikeLinesAfterTheFirstSection() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            == Description

            :note: not a document attribute
            """);

    assertTrue(page.attribute("note").isEmpty());
  }

  @Test
  void readsExplicitAnchors() {
    AsciiDocPage page =
        AsciiDocPage.parse(
            """
            [[geometry-calculator]]
            = Geometry Calculator

            == Description

            Text.

            [#options,role]
            == Options

            Text.
            """);

    assertTrue(page.hasAnchor("geometry-calculator"));
    assertTrue(page.hasAnchor("options"));
    assertFalse(page.hasAnchor("missing"));
  }
}
