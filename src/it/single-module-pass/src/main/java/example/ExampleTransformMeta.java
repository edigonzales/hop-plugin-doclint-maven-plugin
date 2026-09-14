package example;

import org.apache.hop.core.annotations.Transform;

/** Example transform that satisfies the complete documentation contract. */
@Transform(
    id = "EXAMPLE_TRANSFORM",
    name = "Example Transform",
    description = "Demonstrates the documentation contract.",
    image = "example/icons/example-transform.svg",
    categoryDescription = "Examples",
    keywords = {"example", "documentation"},
    documentationUrl = "https://example.org/hop/transforms/example-transform.html")
public class ExampleTransformMeta {}
