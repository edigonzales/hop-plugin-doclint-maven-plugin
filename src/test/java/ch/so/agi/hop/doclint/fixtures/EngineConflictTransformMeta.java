package ch.so.agi.hop.doclint.fixtures;

import org.apache.hop.core.annotations.Transform;

/** Fixture transform that declares supported and excluded engines at the same time. */
@Transform(
    id = "CONFLICT_TRANSFORM",
    name = "Conflict Transform",
    supportedEngines = {"Local"},
    excludedEngines = {"BeamDirectPipelineEngine"})
public class EngineConflictTransformMeta {}
