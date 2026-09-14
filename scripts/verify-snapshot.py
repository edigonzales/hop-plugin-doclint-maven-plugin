#!/usr/bin/env python3
"""Resolve the published snapshot through Maven and run the plugin end to end."""

from __future__ import annotations

import io
import json
import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NS = "{http://maven.apache.org/POM/4.0.0}"
VERSION = ET.parse(ROOT / "pom.xml").getroot().find(NS + "version").text
GROUP_ID = "ch.so.agi"
ARTIFACT_ID = "hop-plugin-doclint-maven-plugin"
# Override for local dry-runs against a file-based repository; CI uses the public snapshot repository.
REPOSITORY_URL = os.environ.get(
    "HOP_PLUGIN_DOCLINT_REPOSITORY", "https://jars.interlis.guru/snapshots/"
)
REPOSITORY = f"sogeo-snapshots::default::{REPOSITORY_URL}"
HOP_VERSION = "2.19.0"


def maven_command(settings: Path, local_repository: Path, *arguments: str) -> list[str]:
    return [
        "mvn",
        "-U",
        "-B",
        "-ntp",
        "-s",
        str(settings),
        "-gs",
        str(settings),
        f"-Dmaven.repo.local={local_repository}",
        *arguments,
    ]


def consumer_pom(fail: bool) -> str:
    return f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>example</groupId>
  <artifactId>doclint-consumer-{"fail" if fail else "pass"}</artifactId>
  <version>1.0.0</version>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>
  <repositories>
    <repository>
      <id>sogeo-snapshots</id>
      <url>{REPOSITORY_URL}</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>sogeo-snapshots</id>
      <url>{REPOSITORY_URL}</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled><updatePolicy>always</updatePolicy></snapshots>
    </pluginRepository>
  </pluginRepositories>
  <dependencies>
    <dependency>
      <groupId>org.apache.hop</groupId>
      <artifactId>hop-engine</artifactId>
      <version>{HOP_VERSION}</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.14.0</version>
      </plugin>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.4.2</version>
      </plugin>
      <plugin>
        <groupId>{GROUP_ID}</groupId>
        <artifactId>{ARTIFACT_ID}</artifactId>
        <version>{VERSION}</version>
        <executions>
          <execution>
            <id>validate-hop-plugin-documentation</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
'''


def write_project(project: Path, fail: bool) -> None:
    source = project / "src/main/java/example/PublishedTransformMeta.java"
    source.parent.mkdir(parents=True)
    source.write_text(
        """package example;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "PUBLISHED_TRANSFORM",
    name = "Published Transform",
    description = "Consumer fixture for the published snapshot.",
    image = "example/icons/published.svg",
    categoryDescription = "Examples",
    keywords = {"published", "snapshot"},
    documentationUrl = "https://example.org/hop/transforms/published-transform.html")
public class PublishedTransformMeta {}
""",
        encoding="utf-8",
    )
    icon = project / "src/main/resources/example/icons/published.svg"
    icon.parent.mkdir(parents=True)
    icon.write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"/>\n', encoding="utf-8")

    page = project / "docs/transforms/published-transform.adoc"
    page.parent.mkdir(parents=True)
    options = "// TODO\n" if fail else "Some option.\n"
    page.write_text(
        f"""= Published Transform
:plugin-id: PUBLISHED_TRANSFORM
:plugin-type: transform
:description: Consumer fixture.

== Description

Checks the published plugin.

== Input

One incoming row per file.

== Options

{options}
== Output

One outgoing row per input row.

== Supported engines

Local.

== Examples

See examples/.

== Error handling

Fails.

== Limitations

None.
""",
        encoding="utf-8",
    )
    example = project / "examples/demo/published-transform.hpl"
    example.parent.mkdir(parents=True)
    example.write_text(
        """<?xml version="1.0" encoding="UTF-8"?>
<pipeline>
  <info>
    <name>published-transform</name>
  </info>
  <transform>
    <type>PUBLISHED_TRANSFORM</type>
    <name>Published Transform</name>
  </transform>
</pipeline>
""",
        encoding="utf-8",
    )
    (project / "pom.xml").write_text(consumer_pom(fail), encoding="utf-8")


def run(project: Path, settings: Path, repository: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        maven_command(settings, repository, "-f", str(project / "pom.xml"), "clean", "verify"),
        cwd=project,
        capture_output=True,
        text=True,
        check=False,
    )


def assert_published_plugin_jar(repository: Path) -> dict[str, str]:
    directory = repository / Path(GROUP_ID.replace(".", "/")) / ARTIFACT_ID / VERSION
    jars = sorted(directory.glob(f"{ARTIFACT_ID}-*.jar"))
    main_jars = [
        jar for jar in jars if "-sources." not in jar.name and "-javadoc." not in jar.name
    ]
    if not main_jars:
        raise AssertionError(f"Maven did not resolve {GROUP_ID}:{ARTIFACT_ID}:{VERSION}")
    published = {}
    for jar in main_jars:
        names = zipfile.ZipFile(io.BytesIO(jar.read_bytes())).namelist()
        if "META-INF/maven/plugin.xml" not in names:
            raise AssertionError(f"Missing Maven plugin descriptor in {jar}")
        if not any(name.endswith("/pom.xml") for name in names):
            raise AssertionError(f"Missing embedded POM in {jar}")
        published[jar.name] = "plugin"
    if not sorted(directory.glob(f"{ARTIFACT_ID}-*.pom")):
        raise AssertionError(f"Maven did not resolve the POM of {GROUP_ID}:{ARTIFACT_ID}:{VERSION}")
    return published


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="hop-plugin-doclint-consumer-") as temporary:
        project = Path(temporary)
        repository = project / "repository"
        settings = project / "settings.xml"
        settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>\n', encoding="utf-8")
        pass_project = project / "pass"
        fail_project = project / "fail"
        write_project(pass_project, fail=False)
        write_project(fail_project, fail=True)

        passed = run(pass_project, settings, repository)
        if passed.returncode != 0:
            raise AssertionError(
                "Expected the published DocLint plugin to pass the valid consumer project.\n"
                + passed.stdout
                + passed.stderr
            )
        for marker in ["Hop Plugin Documentation Check", "Hop plugin documentation validation passed."]:
            if marker not in passed.stdout:
                raise AssertionError(f"Missing '{marker}' in the passing build.\n" + passed.stdout)

        failed = run(fail_project, settings, repository)
        if failed.returncode == 0:
            raise AssertionError("Expected the published DocLint plugin to fail the invalid consumer project.")
        if "section 'Options' is empty" not in failed.stdout:
            raise AssertionError(
                "Missing the expected DocLint error in the failing build.\n" + failed.stdout
            )
        if "Hop plugin documentation validation failed" not in failed.stdout:
            raise AssertionError("Missing the DocLint failure summary.\n" + failed.stdout)

        published = assert_published_plugin_jar(repository)

    result = {"version": VERSION, "artifacts": published}
    (ROOT / "target").mkdir(exist_ok=True)
    (ROOT / "target/published-snapshot.json").write_text(
        json.dumps(result, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
