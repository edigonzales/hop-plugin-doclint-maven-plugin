package ch.so.agi.hop.doclint.model;

/**
 * Result of a single check.
 *
 * <p>{@code elementId} may be blank for repository-wide findings, for example an unreadable example
 * pipeline.
 */
public record CheckResult(String elementId, String check, CheckStatus status, String detail) {

  public static CheckResult ok(String elementId, String check) {
    return new CheckResult(elementId, check, CheckStatus.OK, null);
  }

  public static CheckResult ok(String elementId, String check, String detail) {
    return new CheckResult(elementId, check, CheckStatus.OK, detail);
  }

  public static CheckResult fail(String elementId, String check) {
    return new CheckResult(elementId, check, CheckStatus.FAIL, null);
  }

  public static CheckResult fail(String elementId, String check, String detail) {
    return new CheckResult(elementId, check, CheckStatus.FAIL, detail);
  }

  public static CheckResult warn(String elementId, String check, String detail) {
    return new CheckResult(elementId, check, CheckStatus.WARN, detail);
  }

  public static CheckResult skip(String elementId, String check) {
    return new CheckResult(elementId, check, CheckStatus.SKIP, null);
  }

  public boolean isError() {
    return status == CheckStatus.FAIL;
  }

  public boolean isWarning() {
    return status == CheckStatus.WARN;
  }
}
