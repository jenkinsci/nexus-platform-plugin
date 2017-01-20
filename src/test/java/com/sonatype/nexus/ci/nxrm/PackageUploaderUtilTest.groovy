package com.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.repository.RepositoryManagerClient

import hudson.EnvVars
import hudson.remoting.VirtualChannel
import spock.lang.Specification

class PackageUploaderUtilTest
    extends Specification
{
  def 'it works with no envVars'() {
    setup:
      def client = Mock(RepositoryManagerClient)
      def coordinate = new MavenCoordinate("some-group", "some-artifact", "1.0.0-SNAPSHOT", "jar")
      def asset = new MavenAsset("temp", 'classifier', 'extension')
      def envVars = new EnvVars([:])
      def callable = new PackageUploaderUtil.MavenAssetUploaderCallable(client, "some-repo", coordinate, asset, envVars)
      def file = File.createTempFile("temp", ".tmp")
      def gav = null
      def assets = null

    when:
      callable.invoke(file, Mock(VirtualChannel))

    then:
      1 * client.uploadComponent(*_) >> { args ->
        gav = args[1]
        assets = args[2]
      }

      gav.groupId == 'some-group'
      gav.artifactId == 'some-artifact'
      gav.version == '1.0.0-SNAPSHOT'
      gav.packaging == 'jar'

      assets[0].classifier == 'classifier'
      assets[0].extension == 'extension'
  }

  def 'it expands envVars'() {
    setup:
      def client = Mock(RepositoryManagerClient)
      def coordinate = new MavenCoordinate('$GROUPID', '$ARTIFACTID', '$VERSION', '$PACKAGING')
      def asset = new MavenAsset("temp", '$CLASSIFIER', '$EXTENSION')
      def envVars = new EnvVars(['GROUPID'   : 'some-env-group',
                                 'ARTIFACTID': 'some-env-artifact',
                                 'VERSION'   : '1.0.0-01',
                                 'PACKAGING' : 'jar',
                                 'CLASSIFIER': 'env-classifier',
                                 'EXTENSION' : 'env-extension'])
      def callable = new PackageUploaderUtil.MavenAssetUploaderCallable(client, "some-repo", coordinate, asset, envVars)
      def file = File.createTempFile("temp", ".tmp")
      def gav = null
      def assets = null

    when:
      callable.invoke(file, Mock(VirtualChannel))

    then:
      1 * client.uploadComponent(*_) >> { args ->
        gav = args[1]
        assets = args[2]
      }

      gav.groupId == 'some-env-group'
      gav.artifactId == 'some-env-artifact'
      gav.version == '1.0.0-01'
      gav.packaging == 'jar'

      assets[0].classifier == 'env-classifier'
      assets[0].extension == 'env-extension'
  }

  def 'it ignores undefined envVars'() {
    setup:
      def client = Mock(RepositoryManagerClient)
      def coordinate = new MavenCoordinate('$GROUPID', '$ARTIFACTID', '$VERSION', '$PACKAGING')
      def asset = new MavenAsset("temp", '$CLASSIFIER', '$EXTENSION')
      def envVars = new EnvVars(['GROUPID'  : 'some-env-group',
                                 'PACKAGING': 'jar',
                                 'EXTENSION': 'env-extension'])
      def callable = new PackageUploaderUtil.MavenAssetUploaderCallable(client, "some-repo", coordinate, asset, envVars)
      def file = File.createTempFile("temp", ".tmp")
      def gav = null
      def assets = null

    when:
      callable.invoke(file, Mock(VirtualChannel))

    then:
      1 * client.uploadComponent(*_) >> { args ->
        gav = args[1]
        assets = args[2]
      }

      gav.groupId == 'some-env-group'
      gav.artifactId == '$ARTIFACTID'
      gav.version == '$VERSION'
      gav.packaging == 'jar'

      assets[0].classifier == '$CLASSIFIER'
      assets[0].extension == 'env-extension'
  }
}
