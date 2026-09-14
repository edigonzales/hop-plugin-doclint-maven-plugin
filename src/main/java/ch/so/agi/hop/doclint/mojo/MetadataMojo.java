package ch.so.agi.hop.doclint.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Validates the Hop annotation metadata of compiled plugin classes. */
@Mojo(name = "metadata", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class MetadataMojo extends AbstractDocLintMojo {

  @Override
  protected boolean metadataEnabled() {
    return true;
  }

  @Override
  protected boolean documentationEnabled() {
    return false;
  }

  @Override
  protected boolean samplesEnabled() {
    return false;
  }
}
