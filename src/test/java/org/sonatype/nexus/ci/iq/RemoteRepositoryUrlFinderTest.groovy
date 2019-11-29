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

import java.nio.file.Paths

import com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderBuilder

import hudson.FilePath
import jenkins.model.Jenkins
import org.slf4j.Logger
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

@ConfineMetaClassChanges([IqClientFactory])
class RemoteRepositoryUrlFinderTest
    extends Specification
{
  private final static String url = 'http://a.com/b/c'

  Logger log

  Map<String, String> envVars = new HashMap<>()

  def setup() {
    GroovyMock(Jenkins, global: true)
    Jenkins.instance >> Mock(Jenkins)

    log = Stub()
    envVars.put('GIT_URL', url)
  }

  def 'retrieves the repository URL from the environment variable when set'() {
    setup:
      def remoteRepositoryUrlFinder = new RemoteRepositoryUrlFinder(workspace, log, 'instance-id', 'appId', envVars)

    when:
      String result = remoteRepositoryUrlFinder.call()

    then:
      assert result != null
      assert result == url

    where:
      workspace = new FilePath(new File('/file/path'))
  }

  def 'retrieves the repository URL with jgit when env var not set'() {
    setup:
      def remoteRepositoryUrlFinder = new RemoteRepositoryUrlFinder(workspace, log, 'instance-id', 'appId', null)
      Optional<String> url = new RepositoryUrlFinderBuilder()
          .withGitRepo()
          .build()
          .tryGetRepositoryUrl()

    when:
      String result = remoteRepositoryUrlFinder.call()

    then:
      assert result != null
      assert result == url.get()

    where:
      workspace = new FilePath(new File(Paths.get('').toAbsolutePath().toString()))
  }
}
