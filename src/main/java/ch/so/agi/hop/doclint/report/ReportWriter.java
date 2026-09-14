package ch.so.agi.hop.doclint.report;

import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.CheckStatus;
import ch.so.agi.hop.doclint.model.PluginElement;
import java.util.List;
import org.apache.maven.plugin.logging.Log;

/** Writes the DocLint console report grouped by plugin element. */
public final class ReportWriter {

  private static final String OK = "ok    ";
  private static final String FAIL = "fail  ";
  private static final String WARN = "warn  ";
  private static final String SKIP = "skip  ";

  private final Log log;

  public ReportWriter(Log log) {
    this.log = log;
  }

  public void write(List<PluginElement> elements, List<CheckResult> results) {
    log.info("Hop Plugin Documentation Check");
    results.stream()
        .filter(result -> isBlank(result.elementId()))
        .forEach(this::print);
    for (PluginElement element : elements) {
      List<CheckResult> elementResults =
          results.stream().filter(result -> element.id().equals(result.elementId())).toList();
      if (elementResults.isEmpty()) {
        continue;
      }
      log.info(element.type().label() + ": " + element.id() + " (" + element.className() + ")");
      elementResults.forEach(this::print);
    }
  }

  private void print(CheckResult result) {
    String line = format(result);
    switch (result.status()) {
      case FAIL -> log.error(line);
      case WARN -> log.warn(line);
      case OK, SKIP -> log.info(line);
    }
  }

  private String format(CheckResult result) {
    String label =
        switch (result.status()) {
          case OK -> OK;
          case FAIL -> FAIL;
          case WARN -> WARN;
          case SKIP -> SKIP;
        };
    String message = result.check();
    if (result.detail() != null && !result.detail().isBlank()) {
      message = message + ": " + result.detail();
    }
    return "  " + label + message;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
