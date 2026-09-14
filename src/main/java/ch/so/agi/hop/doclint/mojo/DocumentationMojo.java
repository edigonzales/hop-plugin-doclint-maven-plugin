package ch.so.agi.hop.doclint.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Validates the AsciiDoc pages referenced by the Hop plugin annotations. */
@Mojo(name = "documentation", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class DocumentationMojo extends AbstractDocLintMojo {

  @Override
  protected boolean metadataEnabled() {
    return false;
  }

  @Override
  protected boolean documentationEnabled() {
    return true;
  }

  @Override
  protected boolean samplesEnabled() {
    return false;
  }
}
