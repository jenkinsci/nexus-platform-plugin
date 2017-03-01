/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.quality

import spock.lang.Specification

class CodeQualityTest
    extends Specification
{
  def 'perform static analysis of groovy files using CodeNarc'() {
    expect:
      def ant = new AntBuilder()

      ant.taskdef(name:'codenarc', classname:'org.codenarc.ant.CodeNarcTask')

      ant.codenarc(ruleSetFiles:'org/sonatype/nexus/ci/quality/RuleSetAll.groovy',
          maxPriority1Violations:0, maxPriority2Violations:0) {
        report(type:'ide')
        fileset(dir:'src/main/java') {
          include(name:'**/*.groovy')
        }
      }
  }
}
