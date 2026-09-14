package ch.so.agi.hop.doclint.doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal AsciiDoc reader for the doclint contract: document attributes and level-2 sections.
 *
 * <p>This is intentionally not a full AsciiDoc parser. The linter only needs the contract attributes
 * and whether a required section has readable content.
 */
public final class AsciiDocPage {

  private static final Pattern ATTRIBUTE = Pattern.compile("^\\s*:([^:\\s]+):\\s*(.*?)\\s*$");
  private static final Pattern SECTION = Pattern.compile("^==\\s+(.+?)\\s*$");

  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final Map<String, String> sections = new LinkedHashMap<>();

  private AsciiDocPage() {}

  public static AsciiDocPage read(Path path) throws IOException {
    return parse(Files.readString(path, StandardCharsets.UTF_8));
  }

  public static AsciiDocPage parse(String text) {
    AsciiDocPage page = new AsciiDocPage();
    String currentSection = null;
    StringBuilder currentContent = null;
    for (String line : text.split("\\R", -1)) {
      Matcher attribute = ATTRIBUTE.matcher(line);
      if (attribute.matches() && currentSection == null) {
        page.attributes.put(attribute.group(1).toLowerCase(Locale.ROOT), attribute.group(2).trim());
        continue;
      }
      Matcher section = SECTION.matcher(line);
      if (section.matches()) {
        if (currentSection != null) {
          page.sections.putIfAbsent(currentSection, currentContent.toString());
        }
        currentSection = section.group(1).trim().toLowerCase(Locale.ROOT);
        currentContent = new StringBuilder();
        continue;
      }
      if (currentSection != null) {
        currentContent.append(line).append('\n');
      }
    }
    if (currentSection != null) {
      page.sections.putIfAbsent(currentSection, currentContent.toString());
    }
    return page;
  }

  public Optional<String> attribute(String name) {
    return Optional.ofNullable(attributes.get(name.toLowerCase(Locale.ROOT)));
  }

  public boolean hasSection(String name) {
    return sections.containsKey(name.toLowerCase(Locale.ROOT));
  }

  /** True when the section exists and contains at least one readable line. */
  public boolean hasSectionContent(String name) {
    String content = sections.get(name.toLowerCase(Locale.ROOT));
    if (content == null) {
      return false;
    }
    return content
        .lines()
        .map(String::trim)
        .anyMatch(line -> !line.isEmpty() && !line.startsWith("//"));
  }
}
