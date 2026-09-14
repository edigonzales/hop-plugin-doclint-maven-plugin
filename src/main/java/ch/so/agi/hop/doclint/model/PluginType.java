package ch.so.agi.hop.doclint.model;

import java.util.List;

/**
 * Hop plugin element types understood by the linter.
 *
 * <p>v0.1 scans {@code @Transform} only. Action and metadata rules are already modelled so that
 * support can be enabled without changing the validation core.
 */
public enum PluginType {
  TRANSFORM(
      "transform",
      "transforms",
      "org.apache.hop.core.annotations.Transform",
      List.of(
          "Description",
          "Input",
          "Options",
          "Output",
          "Supported engines",
          "Examples",
          "Error handling",
          "Limitations")),
  ACTION(
      "action",
      "actions",
      "org.apache.hop.core.annotations.Action",
      List.of(
          "Description",
          "Options",
          "Results",
          "Supported engines",
          "Examples",
          "Error handling",
          "Limitations")),
  METADATA(
      "metadata",
      "metadata",
      "org.apache.hop.metadata.api.HopMetadata",
      List.of("Description", "Configuration", "Usage", "Examples", "Limitations"));

  private static final List<PluginType> SCANNED = List.of(TRANSFORM);

  private final String id;
  private final String docsSubdirectory;
  private final String annotationName;
  private final List<String> requiredSections;

  PluginType(
      String id, String docsSubdirectory, String annotationName, List<String> requiredSections) {
    this.id = id;
    this.docsSubdirectory = docsSubdirectory;
    this.annotationName = annotationName;
    this.requiredSections = requiredSections;
  }

  /** Types that are currently discovered on the compiled classes. */
  public static List<PluginType> scanned() {
    return SCANNED;
  }

  public String id() {
    return id;
  }

  public String docsSubdirectory() {
    return docsSubdirectory;
  }

  public String annotationName() {
    return annotationName;
  }

  public List<String> requiredSections() {
    return requiredSections;
  }

  public String label() {
    return Character.toUpperCase(id.charAt(0)) + id.substring(1);
  }
}
