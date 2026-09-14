package example.core;

/** Plain helper class; the module has no Hop plugin annotations and must be skipped. */
public final class CoreHelper {

  private CoreHelper() {}

  public static int add(int left, int right) {
    return left + right;
  }
}
