/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library('zion-pipeline-library')
import com.sonatype.jenkins.pipeline.GitHub
import com.sonatype.jenkins.pipeline.OsTools

node('ubuntu-zion') {
  def commitId, commitDate, pom, version
  GitHub gitHub

  stage('Preparation') {
    deleteDir()

    checkout scm

    commitId = OsTools.runSafe(this, 'git rev-parse HEAD')
    commitDate = OsTools.runSafe(this, "git show -s --format=%cd --date=format:%Y%m%d-%H%M%S ${commitId}")

    OsTools.runSafe(this, 'git config --global user.email sonatype-ci@sonatype.com')
    OsTools.runSafe(this, 'git config --global user.name Sonatype CI')

    pom = readMavenPom file: 'pom.xml'
    version = pom.version.replace("-SNAPSHOT", ".${commitDate}.${commitId.substring(0, 7)}")

    currentBuild.displayName = "#${currentBuild.number} - ${version}"

    def apiToken
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'integrations-github-api',
                      usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_PASSWORD']]) {
      apiToken = env.GITHUB_API_PASSWORD
    }
    gitHub = new GitHub(this, 'jenkinsci/nexus-platform-plugin', apiToken)
  }
  stage('License Check') {
    gitHub.statusUpdate commitId, 'pending', 'license', 'License check is running'

    withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'jenkins-settings.xml') {
      OsTools.runSafe(this, 'mvn license:check')
    }

    if (currentBuild.result == 'FAILURE') {
      gitHub.statusUpdate commitId, 'failure', 'license', 'License check failed'
      return
    } else {
      gitHub.statusUpdate commitId, 'success', 'license', 'License check succeeded'
    }
  }
  stage('Build') {
    gitHub.statusUpdate commitId, 'pending', 'build', 'Build is running'

    withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'jenkins-settings.xml') {
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

    def evaluation = nexusPolicyEvaluation failBuildOnNetworkError: false, iqApplication: 'nexus-jenkins-plugin', iqScanPatterns: [[scanPattern: 'target/nexus-jenkins-plugin.hpi']], iqStage: 'build', jobCredentialsId: ''

    if (currentBuild.result == 'FAILURE') {
      gitHub.statusUpdate commitId, 'failure', 'analysis', 'Nexus Lifecycle Analysis failed', "${evaluation.applicationCompositionReportUrl}"
      return
    } else {
      gitHub.statusUpdate commitId, 'success', 'analysis', 'Nexus Lifecycle Analysis passed', "${evaluation.applicationCompositionReportUrl}"
    }
  }
  stage('Archive Results') {
    junit '**/target/surefire-reports/TEST-*.xml'
    archive 'target/*.hpi'
  }
  if (currentBuild.result == 'FAILURE') {
    return
  }
  stage('Deploy to Sonatype') {
    withGpgCredentials('gnupg'), {
      withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'public-settings.xml') {
        OsTools.runSafe(this, "mvn -Psonatype -Darguments=-DskipTests -DreleaseVersion=${version} -DdevelopmentVersion=${pom.version} -DpushChanges=false -DlocalCheckout=true -DpreparationGoals=initialize release:prepare release:perform -B")
      }
    }
  }
  if (scm.branches[0].name != '*/master')
  {
    return
  }
  input 'Push tags and deploy to Jenkins Update Center?'
  stage('Push tags') {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'integrations-github-api',
                      usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_PASSWORD']]) {
      OsTools.runSafe(this,
          "git push https://${env.GITHUB_API_USERNAME}:${env.GITHUB_API_PASSWORD}@github.com/jenkinsci/nexus-platform-plugin.git ${pom.artifactId}-${version}")
    }

    // Reset all changes to local repository made by the Maven release plugin
    OsTools.runSafe(this, "git tag -d ${pom.artifactId}-${version}")
    OsTools.runSafe(this, 'git clean -f && git reset --hard origin/master')
  }
  stage ('Deploy to Jenkins') {
    withMaven(jdk: 'JDK8u121', maven: 'M3', mavenSettingsConfig: 'jenkins-settings.xml') {
      OsTools.runSafe(this, "mvn -Darguments=-DskipTests -DreleaseVersion=${version} -DdevelopmentVersion=${pom.version} -DpushChanges=false -DlocalCheckout=true -DpreparationGoals=initialize release:prepare release:perform -B")
    }
  }
}
