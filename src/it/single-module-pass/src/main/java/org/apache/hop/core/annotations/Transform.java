package org.apache.hop.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal stand-in for the Apache Hop annotation. Integration test fixtures stay hermetic; the unit
 * tests scan the real annotation from the Hop dependency.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transform {
  String id();

  String name();

  String description() default "";

  String image() default "";

  String categoryDescription() default "";

  String documentationUrl() default "";

  String[] keywords() default {};

  String[] supportedEngines() default {};

  String[] excludedEngines() default {};
}
