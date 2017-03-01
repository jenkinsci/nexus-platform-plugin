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
package org.sonatype.nexus.ci.config

import org.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep
import org.sonatype.nexus.ci.iq.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.iq.PolicyEvaluationProjectAction
import org.sonatype.nexus.ci.iq.ScanPattern
import org.sonatype.nexus.ci.nxrm.MavenAsset
import org.sonatype.nexus.ci.nxrm.MavenCoordinate
import org.sonatype.nexus.ci.nxrm.MavenPackage
import org.sonatype.nexus.ci.nxrm.NexusPublisherBuildStep
import org.sonatype.nexus.ci.nxrm.Package

import hudson.model.Items
import hudson.model.Run
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ComToOrgMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ComToOrgMigrator)

  static void migrateAliases() {
    iqAliases()
    rmAliases()
  }

  @SuppressWarnings('AbcMetric')
  static void migrateGlobalConfiguration() {
    def oldGlobalConfiguration = new com.sonatype.nexus.ci.config.GlobalNexusConfiguration()
    if (oldGlobalConfiguration.exists()) {
      LOGGER.debug(Messages.ComToOrgMigrator_MigratingPackages())

      oldGlobalConfiguration.load()

      def newGlobalConfiguration = GlobalNexusConfiguration.getGlobalNexusConfiguration()
      newGlobalConfiguration.instanceId = oldGlobalConfiguration.instanceId
      newGlobalConfiguration.nxrmConfigs = []
      newGlobalConfiguration.iqConfigs = []

      oldGlobalConfiguration.nxrmConfigs.each { com.sonatype.nexus.ci.config.NxrmConfiguration nxrmConfiguration ->
        def newNxrmConfiguration = new Nxrm2Configuration(nxrmConfiguration.id,
            nxrmConfiguration.internalId, nxrmConfiguration.displayName, nxrmConfiguration.serverUrl,
            nxrmConfiguration.credentialsId)
        newGlobalConfiguration.nxrmConfigs.add(newNxrmConfiguration)
      }
      oldGlobalConfiguration.iqConfigs.each { com.sonatype.nexus.ci.config.NxiqConfiguration iqConfiguration ->
        def newIqConfiguration = new NxiqConfiguration(iqConfiguration.serverUrl,
            iqConfiguration.isPkiAuthentication, iqConfiguration.credentialsId)
        newGlobalConfiguration.iqConfigs.add(newIqConfiguration)
      }

      newGlobalConfiguration.save()
      oldGlobalConfiguration.delete()

      LOGGER.debug(Messages.ComToOrgMigrator_MigratingPackagesFinished(newGlobalConfiguration.nxrmConfigs.size(),
          newGlobalConfiguration.iqConfigs.size()))
    }
  }

  static void iqAliases() {
    def comToOrgMappings = [
        'com.sonatype.nexus.ci.iq.IqPolicyEvaluatorBuildStep'   : IqPolicyEvaluatorBuildStep,
        'com.sonatype.nexus.ci.iq.PolicyEvaluationHealthAction' : PolicyEvaluationHealthAction,
        'com.sonatype.nexus.ci.iq.PolicyEvaluationProjectAction': PolicyEvaluationProjectAction,
        'com.sonatype.nexus.ci.iq.ScanPattern'                  : ScanPattern
    ]

    comToOrgMappings.each { key, value ->
      Items.XSTREAM2.addCompatibilityAlias(key, value)
      Run.XSTREAM2.addCompatibilityAlias(key, value)
    }

    def iqPolicyEvaluatorTraitFieldMappings = [
        'com_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqStage'                :
            'org_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqStage',
        'com_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqApplication'          :
            'org_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqApplication',
        'com_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqScanPatterns'         :
            'org_sonatype_nexus_ci_iq_IqPolicyEvaluator__iqScanPatterns',
        'com_sonatype_nexus_ci_iq_IqPolicyEvaluator__failBuildOnNetworkError':
            'org_sonatype_nexus_ci_iq_IqPolicyEvaluator__failBuildOnNetworkError',
        'com_sonatype_nexus_ci_iq_IqPolicyEvaluator__jobCredentialsId'       :
            'org_sonatype_nexus_ci_iq_IqPolicyEvaluator__jobCredentialsId'
    ]

    iqPolicyEvaluatorTraitFieldMappings.each { key, value ->
      Items.XSTREAM2.aliasField(key, IqPolicyEvaluatorBuildStep, value)
      Run.XSTREAM2.aliasField(key, IqPolicyEvaluatorBuildStep, value)
    }
  }

  static void rmAliases() {
    def comToOrgMappings = [
        'com.sonatype.nexus.ci.nxrm.MavenAsset'             : MavenAsset,
        'com.sonatype.nexus.ci.nxrm.MavenCoordinate'        : MavenCoordinate,
        'com.sonatype.nexus.ci.nxrm.MavenPackage'           : MavenPackage,
        'com.sonatype.nexus.ci.nxrm.NexusPublisherBuildStep': NexusPublisherBuildStep,
        'com.sonatype.nexus.ci.nxrm.Package'                : Package
    ]

    comToOrgMappings.each { key, value ->
      Items.XSTREAM2.addCompatibilityAlias(key, value)
      Run.XSTREAM2.addCompatibilityAlias(key, value)
    }
  }
}
