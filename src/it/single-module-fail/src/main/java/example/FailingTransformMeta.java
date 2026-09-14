package example;

import org.apache.hop.core.annotations.Transform;

/** Example transform with incomplete metadata and incomplete documentation. */
@Transform(
    id = "FAILING_TRANSFORM",
    name = "Failing Transform",
    description = "Fails the documentation checks.",
    image = "example/icons/missing.svg",
    categoryDescription = "Examples",
    documentationUrl = "https://example.org/hop/transforms/failing-transform.html")
public class FailingTransformMeta {}
