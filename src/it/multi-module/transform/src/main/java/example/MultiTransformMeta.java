package example;

import org.apache.hop.core.annotations.Transform;

/** Transform in a module; the documentation lives at the repository root. */
@Transform(
    id = "MULTI_TRANSFORM",
    name = "Multi Transform",
    description = "Checks multi-module documentation resolution.",
    image = "example/icons/multi-transform.svg",
    categoryDescription = "Examples",
    keywords = {"multi", "module"},
    documentationUrl = "https://example.org/hop/transforms/multi-transform.html")
public class MultiTransformMeta {}
