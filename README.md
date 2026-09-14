# hop-plugin-doclint-maven-plugin

Maven plugin that validates Apache Hop plugin annotations against the documentation and examples in
the repository. It is the automated part of the hop plugin documentation contract.

## Documentation contract

| Location | Purpose | Format |
| --- | --- | --- |
| `docs/` | Canonical technical documentation, one page per plugin element | AsciiDoc with `:plugin-id:` / `:plugin-type:` and the required sections |
| `examples/` | Small user examples that can be opened in the Hop GUI | Markdown `README.md` plus `.hpl` pipelines and test data |
| `e2e/` | Automated integration tests for CI | Not part of the documentation contract |

Every `@Transform` must link its canonical page through `documentationUrl`. The page is derived by
convention; there is no mapping file:

* a page URL uses the file name: `.../transforms/example-transform.html` maps to
  `docs/transforms/example-transform.adoc`
* a single-page handbook URL uses the fragment: `.../geometry-calculator/main/index.html#geometry-calculator`
  maps to `docs/transforms/geometry-calculator.adoc` and requires the explicit anchor
  `[[geometry-calculator]]` in that file

The annotation `id` and the `:plugin-id:` attribute must match in both cases.

## Checks in v0.1

The linter scans `target/classes` with [ClassGraph](https://github.com/classgraph/classgraph). The
plugin classes are never loaded or instantiated, so no Hop runtime, SWT or native library is
initialized.

| Area | Check | Severity |
| --- | --- | --- |
| Annotation | `id`, `name`, `description`, `image`, `categoryDescription` present | error |
| Annotation | `keywords` present (configurable) | error |
| Annotation | `documentationUrl` present (configurable) | error |
| Annotation | Referenced image resource exists in the compiled classes | error |
| Annotation | `supportedEngines` and `excludedEngines` are not both set | error |
| Annotation | Deprecated `id1,id2` aliases | warning |
| Documentation | Page exists, `:plugin-id:` matches, `:plugin-type: transform`, `:description:` present | error |
| Documentation | The URL fragment of a single-page handbook URL exists as explicit anchor in the page | error |
| Documentation | Required sections present and not empty: Description, Input, Options, Output, Supported engines, Examples, Error handling, Limitations | error |
| Examples | At least one `.hpl` under `examples/` references the plugin id (configurable) | error |
| Examples | Every `.hpl` under `examples/` is readable XML | error |

Deferred to later versions: i18n key lookup, `@Action` and `@HopMetadata` rules, screenshots,
remote URL checks and action/metadata section rules.

## Goals

| Goal | Phase | Purpose |
| --- | --- | --- |
| `hop-plugin-doclint:metadata` | `verify` | Annotation metadata only |
| `hop-plugin-doclint:documentation` | `verify` | AsciiDoc pages only |
| `hop-plugin-doclint:samples` | `verify` | User examples only |
| `hop-plugin-doclint:check` | `verify` | Runs all checks; wired into `verify` by `hop-plugin-parent` |
| `hop-plugin-doclint:help` | | Parameter help |

## Configuration

| Parameter | Property | Default |
| --- | --- | --- |
| `docsDirectory` | `hop.plugin.doclint.docsDirectory` | `<top-level project>/docs` |
| `examplesDirectory` | `hop.plugin.doclint.examplesDirectory` | `<top-level project>/examples` |
| `requireKeywords` | `hop.plugin.doclint.requireKeywords` | `true` |
| `requireDocumentation` | `hop.plugin.doclint.requireDocumentation` | `true` |
| `requireExamples` | `hop.plugin.doclint.requireExamples` | `true` |
| `skip` | `hop.plugin.doclint.skip` | `false` |

## Usage

Plugin repositories inherit the configuration from
[`ch.so.agi:hop-plugin-parent`](https://github.com/edigonzales/hop-plugin-parent) and do not
configure the linter. That parent also declares
`https://jars.interlis.guru/snapshots/` as a `pluginRepository`; Maven resolves build plugins
exclusively through plugin repositories, so standalone users must add one. Without the parent:

```xml
<plugin>
  <groupId>ch.so.agi</groupId>
  <artifactId>hop-plugin-doclint-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <id>validate-hop-plugin-documentation</id>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Documentation-only iteration without running the full test suite:

```bash
mvn -pl hop-transform-geometry-calculator hop-plugin-doclint:check
```

The goal runs once per module. Modules without Hop plugin annotations are reported as skipped, so
multi-module repositories with core and assembly modules work without extra configuration.
`docs/` and `examples/` are resolved against the top-level project, which is why the checks should
run from the reactor root (`mvn verify` or `mvn -pl <module> ...`).

## Report

```text
[INFO] Hop Plugin Documentation Check
[INFO] Transform: GEOMETRY_CALCULATOR_TRANSFORM (ch.so.agi.hop.geometry.calculator.transform.GeometryCalculatorMeta)
[INFO]   ok    id: GEOMETRY_CALCULATOR_TRANSFORM
[INFO]   ok    name: Geometry Calculator
[ERROR]   fail  keywords are missing
[ERROR]   fail  required section 'Options' is empty in docs/transforms/geometry-calculator.adoc
[ERROR]   fail  no example uses GEOMETRY_CALCULATOR_TRANSFORM: add an .hpl pipeline under examples
[ERROR] Hop plugin documentation validation failed: 3 error(s), 0 warning(s).
```

Warnings do not fail the build.

## Build

```bash
mvn clean verify
```

Requires Java 21 and Maven. `verify` runs the unit tests and the Maven Invoker integration tests
under `src/it/`. Integration tests are hermetic: they use a minimal local annotation stub and do not
download Hop.

## Publication

Snapshots are published to `https://jars.interlis.guru/snapshots/` through the shared
`hop-plugin-ci` library workflows:

```text
ch.so.agi:hop-plugin-doclint-maven-plugin:0.1.0-SNAPSHOT
```

Publishing runs on pushes to `main` and manual workflow runs; pull requests verify only. After
publication, `scripts/verify-snapshot.py` resolves the snapshot in a fresh local repository and runs
the plugin against a consumer project.

## License

MIT
