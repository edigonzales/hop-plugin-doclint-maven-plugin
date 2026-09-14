package ch.so.agi.hop.doclint.scan;

import ch.so.agi.hop.doclint.model.PluginElement;
import ch.so.agi.hop.doclint.model.PluginType;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers Hop plugin annotations on compiled classes.
 *
 * <p>ClassGraph parses the class files without loading or instantiating the plugin classes. The Hop
 * annotation classes themselves do not have to be present: only their fully qualified names are
 * compared.
 */
public final class PluginElementScanner {

  /** Discovered plugin elements and the number of classes that were scanned. */
  public record ScanOutcome(List<PluginElement> elements, int scannedClasses) {}

  public ScanOutcome scan(Path classesDirectory) throws IOException {
    List<PluginElement> elements = new ArrayList<>();
    try (ScanResult scanResult =
        new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .ignoreParentClassLoaders()
            // Pass the File: Path implements Iterable<Path>, and the Iterable overload of
            // overrideClasspath would treat every path segment as a classpath element.
            .overrideClasspath(classesDirectory.toFile())
            .scan()) {
      for (PluginType type : PluginType.scanned()) {
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(type.annotationName())) {
          elements.add(toPluginElement(type, classInfo));
        }
      }
      return new ScanOutcome(elements, scanResult.getAllClasses().size());
    }
  }

  private PluginElement toPluginElement(PluginType type, ClassInfo classInfo) {
    AnnotationInfo annotation = classInfo.getAnnotationInfo(type.annotationName());
    Map<String, Object> attributes = new LinkedHashMap<>();
    if (annotation != null) {
      AnnotationParameterValueList values = annotation.getParameterValues();
      if (values != null) {
        for (AnnotationParameterValue value : values) {
          attributes.put(value.getName(), value.getValue());
        }
      }
    }
    Object rawId = attributes.get("id");
    return new PluginElement(
        type, rawId == null ? "" : String.valueOf(rawId), classInfo.getName(), attributes);
  }
}
