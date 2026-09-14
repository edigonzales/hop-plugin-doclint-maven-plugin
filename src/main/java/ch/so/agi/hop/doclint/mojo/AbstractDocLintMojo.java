package ch.so.agi.hop.doclint.mojo;

import ch.so.agi.hop.doclint.config.DocLintConfig;
import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.PluginElement;
import ch.so.agi.hop.doclint.report.ReportWriter;
import ch.so.agi.hop.doclint.scan.PluginElementScanner;
import ch.so.agi.hop.doclint.validate.DocumentationValidator;
import ch.so.agi.hop.doclint.validate.MetadataValidator;
import ch.so.agi.hop.doclint.validate.SampleValidator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Base implementation of the DocLint goals.
 *
 * <p>Every module runs the goals against its own compiled classes. Modules without Hop plugin
 * annotations are skipped, which makes the configuration work for multi-module repositories with
 * core and assembly modules. {@code docs/} and {@code examples/} default to the top-level project
 * directory because they live next to the aggregator POM.
 */
public abstract class AbstractDocLintMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  protected MavenSession session;

  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
  protected File classesDirectory;

  @Parameter(property = "hop.plugin.doclint.docsDirectory")
  protected File docsDirectory;

  @Parameter(property = "hop.plugin.doclint.examplesDirectory")
  protected File examplesDirectory;

  @Parameter(property = "hop.plugin.doclint.requireKeywords", defaultValue = "true")
  protected boolean requireKeywords;

  @Parameter(property = "hop.plugin.doclint.requireDocumentation", defaultValue = "true")
  protected boolean requireDocumentation;

  @Parameter(property = "hop.plugin.doclint.requireExamples", defaultValue = "true")
  protected boolean requireExamples;

  @Parameter(property = "hop.plugin.doclint.skip", defaultValue = "false")
  protected boolean skip;

  protected abstract boolean metadataEnabled();

  protected abstract boolean documentationEnabled();

  protected abstract boolean samplesEnabled();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Hop plugin documentation check is skipped.");
      return;
    }
    if (classesDirectory == null || !classesDirectory.isDirectory()) {
      getLog()
          .info(
              "No compiled classes in "
                  + classesDirectory
                  + " - Hop plugin documentation check skipped.");
      return;
    }
    List<PluginElement> elements;
    try {
      PluginElementScanner.ScanOutcome outcome =
          new PluginElementScanner().scan(classesDirectory.toPath());
      elements = outcome.elements();
      getLog()
          .debug("Scanned " + outcome.scannedClasses() + " class(es) in " + classesDirectory + ".");
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to scan " + classesDirectory + ".", e);
    }
    if (elements.isEmpty()) {
      getLog()
          .info(
              "No Hop plugin annotations found in "
                  + classesDirectory
                  + " - Hop plugin documentation check skipped.");
      return;
    }

    DocLintConfig config = createConfig();
    List<CheckResult> results = new ArrayList<>();
    try {
      if (metadataEnabled()) {
        results.addAll(
            new MetadataValidator().validate(elements, config, classesDirectory.toPath()));
      }
      if (documentationEnabled()) {
        results.addAll(new DocumentationValidator().validate(elements, config));
      }
      if (samplesEnabled()) {
        results.addAll(new SampleValidator().validate(elements, config));
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to read documentation or examples.", e);
    }

    new ReportWriter(getLog()).write(elements, results);

    long errors = results.stream().filter(CheckResult::isError).count();
    long warnings = results.stream().filter(CheckResult::isWarning).count();
    if (errors > 0) {
      throw new MojoFailureException(
          "Hop plugin documentation validation failed: "
              + errors
              + " error(s), "
              + warnings
              + " warning(s).");
    }
    if (warnings > 0) {
      getLog()
          .warn("Hop plugin documentation validation passed with " + warnings + " warning(s).");
    } else {
      getLog().info("Hop plugin documentation validation passed.");
    }
  }

  private DocLintConfig createConfig() {
    Path root =
        session.getTopLevelProject() != null && session.getTopLevelProject().getBasedir() != null
            ? session.getTopLevelProject().getBasedir().toPath()
            : project.getBasedir().toPath();
    Path docs = docsDirectory != null ? docsDirectory.toPath() : root.resolve("docs");
    Path examples = examplesDirectory != null ? examplesDirectory.toPath() : root.resolve("examples");
    return new DocLintConfig(
        root.toAbsolutePath().normalize(),
        docs,
        examples,
        requireKeywords,
        requireDocumentation,
        requireExamples);
  }
}
