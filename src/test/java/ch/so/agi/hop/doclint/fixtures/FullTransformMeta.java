package ch.so.agi.hop.doclint.fixtures;

import org.apache.hop.core.annotations.Transform;

/** Fixture transform with every attribute the linter checks. */
@Transform(
    id = "FULL_TRANSFORM",
    name = "Full Transform",
    description = "Fixture with all attributes set",
    image = "ch/so/agi/hop/doclint/fixtures/full-transform.svg",
    categoryDescription = "Fixtures",
    keywords = {"fixture", "documentation"},
    documentationUrl = "https://example.org/hop/transforms/full-transform.html",
    supportedEngines = {"Local"})
public class FullTransformMeta {}
