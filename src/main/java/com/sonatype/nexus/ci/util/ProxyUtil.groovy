/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.util

import java.util.regex.Pattern

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ServerConfig

import hudson.ProxyConfiguration
import org.apache.http.client.utils.URIBuilder

class ProxyUtil
{
  static boolean shouldProxyForUri(ProxyConfiguration proxy, URI uri) {
    !proxy.noProxyHostPatterns?.find() { Pattern pattern -> uri.host ==~ pattern }
  }

  static ServerConfig buildProxyConfig(ProxyConfiguration proxy) {
    def proxyUri = new URIBuilder(proxy.name).setPort(proxy.port).build()

    if(proxy.userName) {
      def authentication = new Authentication(proxy.userName, proxy.password)
      return new ServerConfig(proxyUri, authentication)
    } else {
      return new ServerConfig(proxyUri)
    }
  }
}
