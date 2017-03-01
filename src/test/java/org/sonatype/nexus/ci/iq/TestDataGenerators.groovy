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
package org.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ComponentDisplayName
import com.sonatype.nexus.api.iq.ComponentDisplayNamePart
import com.sonatype.nexus.api.iq.ComponentFact
import com.sonatype.nexus.api.iq.ComponentIdentifier
import com.sonatype.nexus.api.iq.ConditionFact
import com.sonatype.nexus.api.iq.ConstraintFact
import com.sonatype.nexus.api.iq.PolicyAlert
import com.sonatype.nexus.api.iq.PolicyFact

import static java.util.Collections.singletonList

class TestDataGenerators
{
  static PolicyAlert createAlert(final String actionTypeId) {
    Action action = new Action(actionTypeId, "target", "targetType")
    ComponentDisplayNamePart componentDisplayNamePart = new ComponentDisplayNamePart("field", "value")
    ComponentDisplayName componentDisplayName = new ComponentDisplayName(singletonList(componentDisplayNamePart))
    ConditionFact conditionFact = new ConditionFact("conditionTypeId", "summary", "reason")
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "operatorName",
        singletonList(conditionFact))
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("format", new TreeMap<String, String>())
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "12hash34", singletonList(constraintFact),
        Collections.<String> emptyList(), componentDisplayName)
    PolicyFact trigger = new PolicyFact("policyId", "policyName", 5, singletonList(componentFact))
    new PolicyAlert(trigger, singletonList(action))
  }
}
