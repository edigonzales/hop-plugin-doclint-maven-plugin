package ch.so.agi.hop.doclint.config;

import java.nio.file.Path;

/**
 * Resolved DocLint settings for one Maven project.
 *
 * <p>Directories default to the top-level Maven project because {@code docs/} and {@code examples/}
 * live next to the aggregator POM, not inside a plugin module.
 */
public record DocLintConfig(
    Path repositoryRoot,
    Path docsDirectory,
    Path examplesDirectory,
    boolean requireKeywords,
    boolean requireDocumentation,
    boolean requireExamples) {

  /** Renders a path relative to the repository root when possible, for stable messages. */
  public String display(Path path) {
    Path absolute = path.toAbsolutePath().normalize();
    Path root = repositoryRoot.toAbsolutePath().normalize();
    if (absolute.startsWith(root)) {
      return root.relativize(absolute).toString().replace('\\', '/');
    }
    return absolute.toString();
  }
}
