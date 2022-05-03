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

import com.sonatype.nexus.api.common.Authentication
import com.sonatype.nexus.api.common.ProxyConfig

import hudson.ProxyConfiguration

class ProxyUtil
{
  static boolean shouldProxyForUri(ProxyConfiguration proxy, URI uri) {
    !proxy.noProxyHostPatterns?.find { Pattern pattern -> uri.host ==~ pattern }
  }

  static ProxyConfig newProxyConfig(ProxyConfiguration proxy) {
    def noProxyHostsList = getNoProxyHostsList(proxy.noProxyHost)
    def authentication = getProxyAuthentication(proxy)
    return new ProxyConfig(proxy.name, proxy.port, authentication, noProxyHostsList)
  }

  static Authentication getProxyAuthentication(ProxyConfiguration proxy) {
    return proxy.userName ? new Authentication(proxy.userName, proxy.password) : null
  }

  static List<String> getNoProxyHostsList(String noProxyHost) {
    return noProxyHost ? Arrays.asList(noProxyHost.split('[ \t\n,|]+')) :Collections.emptyList()
  }
}
