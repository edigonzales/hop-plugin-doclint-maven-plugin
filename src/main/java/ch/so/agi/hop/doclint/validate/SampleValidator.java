package ch.so.agi.hop.doclint.validate;

import ch.so.agi.hop.doclint.config.DocLintConfig;
import ch.so.agi.hop.doclint.model.CheckResult;
import ch.so.agi.hop.doclint.model.PluginElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Validates that every plugin element has a user example under {@code examples/}.
 *
 * <p>v0.1 checks two things: at least one readable {@code .hpl} pipeline exists and it references
 * the plugin element id in a {@code <type>} element. The example is parsed as XML; a broken sample
 * is reported as an error.
 */
public final class SampleValidator {

  public List<CheckResult> validate(List<PluginElement> elements, DocLintConfig config)
      throws IOException {
    List<CheckResult> results = new ArrayList<>();
    Path examples = config.examplesDirectory();
    List<Path> samples = listSamples(examples);
    Map<Path, Set<String>> usedIds = new LinkedHashMap<>();
    for (Path sample : samples) {
      try {
        usedIds.put(sample, referencedTransformIds(sample));
      } catch (SAXException | ParserConfigurationException | IOException e) {
        results.add(
            CheckResult.fail(
                "",
                "example pipeline is not readable: " + config.display(sample),
                e.getMessage()));
      }
    }
    for (PluginElement element : elements) {
      results.add(checkElement(element, config, examples, usedIds));
    }
    return results;
  }

  private CheckResult checkElement(
      PluginElement element,
      DocLintConfig config,
      Path examples,
      Map<Path, Set<String>> usedIds) {
    Path match =
        usedIds.entrySet().stream()
            .filter(entry -> entry.getValue().contains(element.id()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    if (match != null) {
      return CheckResult.ok(element.id(), "example", config.display(match));
    }
    if (!config.requireExamples()) {
      return CheckResult.skip(element.id(), "example (not required)");
    }
    if (!Files.isDirectory(examples)) {
      return CheckResult.fail(
          element.id(),
          "examples directory is missing",
          config.display(examples) + " must contain at least one .hpl using " + element.id());
    }
    return CheckResult.fail(
        element.id(),
        "no example uses " + element.id(),
        "add an .hpl pipeline under " + config.display(examples));
  }

  private List<Path> listSamples(Path examples) throws IOException {
    if (!Files.isDirectory(examples)) {
      return List.of();
    }
    try (Stream<Path> files = Files.walk(examples)) {
      return files.filter(Files::isRegularFile).filter(SampleValidator::isPipeline).sorted().toList();
    }
  }

  private static boolean isPipeline(Path path) {
    return path.getFileName().toString().toLowerCase().endsWith(".hpl");
  }

  private Set<String> referencedTransformIds(Path file)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setErrorHandler(
        new ErrorHandler() {
          @Override
          public void warning(SAXParseException e) {}

          @Override
          public void error(SAXParseException e) {}

          @Override
          public void fatalError(SAXParseException e) {}
        });
    Document document = builder.parse(file.toFile());

    Set<String> ids = new LinkedHashSet<>();
    NodeList transforms = document.getElementsByTagName("transform");
    for (int i = 0; i < transforms.getLength(); i++) {
      Element transform = (Element) transforms.item(i);
      String type = childText(transform, "type");
      if (type != null && !type.isBlank()) {
        ids.add(type.trim());
      }
    }
    return ids;
  }

  private static String childText(Element parent, String childName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE
          && childName.equals(child.getNodeName())) {
        return child.getTextContent();
      }
    }
    return null;
  }
}
