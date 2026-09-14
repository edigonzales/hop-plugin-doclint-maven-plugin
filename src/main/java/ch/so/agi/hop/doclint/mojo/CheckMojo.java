package ch.so.agi.hop.doclint.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Runs the metadata, documentation and example checks; the goal wired into {@code verify}. */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo extends AbstractDocLintMojo {

  @Override
  protected boolean metadataEnabled() {
    return true;
  }

  @Override
  protected boolean documentationEnabled() {
    return true;
  }

  @Override
  protected boolean samplesEnabled() {
    return true;
  }
}
