/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library('zion-pipeline-library')
import com.sonatype.jenkins.pipeline.GitHub
import com.sonatype.jenkins.pipeline.OsTools

node {
  def commitId
  GitHub gitHub

  stage('Preparation') {
    checkout scm
    sh 'git rev-parse HEAD > .git/commit-id'
    commitId = readFile('.git/commit-id')

    def apiToken
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'integrations-github-api',
                      usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_PASSWORD']]) {
      apiToken = env.GITHUB_API_PASSWORD
    }
    gitHub = new GitHub(this, 'jenkinsci/nexus-platform-plugin', apiToken)
  }
  stage('License Check') {
    withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'private-settings.xml') {
      OsTools.runSafe(this, 'mvn license:check')
    }
  }
  stage('Build') {
    gitHub.statusUpdate commitId, 'pending', 'build', 'Build in running'

    withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'private-settings.xml') {
      OsTools.runSafe(this, 'mvn clean package')
    }

    if (currentBuild.result == 'FAILURE') {
      gitHub.statusUpdate commitId, 'failure', 'build', 'Build failed'
      return
    } else {
      gitHub.statusUpdate commitId, 'success', 'build', 'Build succeeded'
    }
  }
  stage('Nexus Lifecycle Analysis') {
    gitHub.statusUpdate commitId, 'pending', 'analysis', 'Nexus Lifecycle Analysis in running'

    def evaluation = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'nexus-jenkins-plugin', iqStage: 'build', jobCredentialsId: ''

    if (currentBuild.result == 'FAILURE') {
      gitHub.statusUpdate commitId, 'failure', 'analysis', 'Nexus Lifecycle Analysis failed', "${evaluation.applicationCompositionReportUrl}"
      return
    } else {
      gitHub.statusUpdate commitId, 'success', 'analysis', 'Nexus Lifecycle Analysis passed', "${evaluation.applicationCompositionReportUrl}"
    }
  }
  stage('Results') {
    junit '**/target/surefire-reports/TEST-*.xml'
    archive 'target/*.jar'
  }
}
