/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import java.util.regex.Pattern

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ProxyConfig

import hudson.ProxyConfiguration

@SuppressWarnings('FactoryMethodName') // TODO ignore naming convention in existing code, refactor when convenient
class ProxyUtil
{
  static boolean shouldProxyForUri(ProxyConfiguration proxy, URI uri) {
    !proxy.noProxyHostPatterns?.find { Pattern pattern -> uri.host ==~ pattern }
  }

  static ProxyConfig buildProxyConfig(ProxyConfiguration proxy) {
    if (proxy.userName) {
      def authentication = new Authentication(proxy.userName, proxy.password)
      return new ProxyConfig(proxy.name, proxy.port, authentication)
    } else {
      return new ProxyConfig(proxy.name, proxy.port)
    }
  }
}
