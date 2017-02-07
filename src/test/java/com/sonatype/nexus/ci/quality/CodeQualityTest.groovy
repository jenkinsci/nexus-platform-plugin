/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.quality

import spock.lang.Specification

class CodeQualityTest
    extends Specification
{
  def 'perform static analysis of groovy files using CodeNarc'() {
    expect:
      def ant = new AntBuilder()

      ant.taskdef(name:'codenarc', classname:'org.codenarc.ant.CodeNarcTask')

      ant.codenarc(ruleSetFiles:'com/sonatype/nexus/ci/quality/RuleSetAll.groovy',
          maxPriority1Violations:0, maxPriority2Violations:0) {
        report(type:'ide')
        fileset(dir:'src/main/java') {
          include(name:'**/*.groovy')
        }
      }
  }
}
