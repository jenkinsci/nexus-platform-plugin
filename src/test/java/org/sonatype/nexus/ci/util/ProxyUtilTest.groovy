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
package org.sonatype.nexus.ci.util

import java.util.regex.Pattern

import hudson.ProxyConfiguration
import spock.lang.Specification

class ProxyUtilTest
    extends Specification
{
  def 'should proxy for anything when noProxyHostPatterns is null'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.noProxyHostPatterns >> null

    expect:
      ProxyUtil.shouldProxyForUri(jenkinsProxy, new URI('http://localhost'))
  }

  def 'should proxy for anything when noProxyHostPatterns is empty list'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.noProxyHostPatterns >> []

    expect:
      ProxyUtil.shouldProxyForUri(jenkinsProxy, new URI('http://localhost'))
  }

  def 'should proxy for anything when noProxyHostPatterns matches URI'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.noProxyHostPatterns >> [Pattern.compile('.*.sonatype.com'), Pattern.compile('github.com'), Pattern.compile('localhost')]

    expect:
      ProxyUtil.shouldProxyForUri(jenkinsProxy, new URI(uri)) == shouldProxy

    where:
      uri                              | shouldProxy
      'http://localhost'               | false
      'http://google.com'              | true
      'http://github.com/sonatype'     | false
      'http://repository.sonatype.com' | false
  }

  def 'builds proxy config with no authentication'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.userName >> null
      jenkinsProxy.name >> 'localhost'
      jenkinsProxy.port >> 9080

    when:
      def proxyConfig = ProxyUtil.newProxyConfig(jenkinsProxy)

    then:
      proxyConfig.host == 'localhost'
      proxyConfig.port == 9080
      proxyConfig.authentication == null
      proxyConfig.noProxyHosts == []
  }

  def 'builds proxy config with no proxy hosts'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.userName >> null
      jenkinsProxy.name >> 'localhost'
      jenkinsProxy.port >> 9080
      jenkinsProxy.noProxyHost >> '*.demo'

    when:
      def proxyConfig = ProxyUtil.newProxyConfig(jenkinsProxy)

    then:
      proxyConfig.host == 'localhost'
      proxyConfig.port == 9080
      proxyConfig.authentication == null
      proxyConfig.noProxyHosts == ['*.demo']
  }

  def 'builds proxy config with authentication'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.userName >> 'proxy-user'
      jenkinsProxy.password >> 'proxy-pass'
      jenkinsProxy.name >> 'localhost'
      jenkinsProxy.port >> 9080

    when:
      def proxyConfig = ProxyUtil.newProxyConfig(jenkinsProxy)

    then:
      proxyConfig.host == 'localhost'
      proxyConfig.port == 9080
      proxyConfig.authentication.username == 'proxy-user'
      proxyConfig.authentication.password as String == 'proxy-pass'
      proxyConfig.noProxyHosts == []
  }
}
