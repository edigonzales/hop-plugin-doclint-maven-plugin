package ch.so.agi.hop.doclint.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Validates the user examples under {@code examples/} for every Hop plugin element. */
@Mojo(name = "samples", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SamplesMojo extends AbstractDocLintMojo {

  @Override
  protected boolean metadataEnabled() {
    return false;
  }

  @Override
  protected boolean documentationEnabled() {
    return false;
  }

  @Override
  protected boolean samplesEnabled() {
    return true;
  }
}
