/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

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
      jenkinsProxy.name >> "http://localhost"
      jenkinsProxy.port >> 9080

    when:
      def serverConfig = ProxyUtil.buildProxyConfig(jenkinsProxy)

    then:
      serverConfig.address.host == 'localhost'
      serverConfig.address.port == 9080
      serverConfig.authentication == null
  }

  def 'builds proxy config with authentication'() {
    setup:
      def jenkinsProxy = GroovyMock(ProxyConfiguration)
      jenkinsProxy.userName >> 'proxy-user'
      jenkinsProxy.password >> 'proxy-pass'
      jenkinsProxy.name >> "http://localhost"
      jenkinsProxy.port >> 9080

    when:
      def serverConfig = ProxyUtil.buildProxyConfig(jenkinsProxy)

    then:
      serverConfig.address.host == 'localhost'
      serverConfig.address.port == 9080
      serverConfig.authentication.username == 'proxy-user'
      serverConfig.authentication.password as String == 'proxy-pass'
  }
}
