/*
 * Copyright (c) 2019-present Sonatype, Inc. All rights reserved.
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
package org.sonatype.nexus.ci.nvs.NvsMessageAction

def t = namespace(lib.JenkinsTagLib)

style('''
  #nvs-coming-soon {
    padding-right: 20px;
    padding-top: 10px;
    position: relative;
  }
  .nexus-plugin-nvs-hide-link {
    color: gray !important;
    font-size: 15px;
    padding: 0 10px 10px 10px;
    position: absolute;
    right: 0;
    text-decoration: none !important;
    top: 0;
  }
''')

def nvsMessage = {
  div() {
    span('Sonatype is building an application scanner for Jenkins.')
    br()
    a(href: "https://www.sonatype.com/nvsforjenkins", target: "_blank", "Learn more")
    span(" about what's coming to the Nexus Platform Plugin.")
    div() {
      a(class: "nexus-plugin-nvs-hide-link", href: "#", onclick: "NexusPluginNVS.hideComingSoon(); return false;",
          title: "Close", "x")
    }
  }
}

table(id: 'nvs-coming-soon') {
  t.summary(icon: '/plugin/nexus-jenkins-plugin/images/96x96/sonatype-logo.png', nvsMessage)
}

script {
  raw('''
    var NexusPluginNVS = (function() {
      'use strict';

      var cookieName = "_nexus_plugin_nvs_coming_soon";
      var cookie = cookieName + "=true; path=/; expires=Tue, 19 Jan 2038 03:14:07 GMT";  // max date
      var comingSoon = document.getElementById('nvs-coming-soon');

      function shouldHideComingSoon() {
        return document.cookie.indexOf(cookieName) > -1;
      }

      function hideComingSoon() {
        comingSoon.style.display = 'none';
        document.cookie = cookie;
      }

      function showComingSoon() {
        comingSoon.style.display = 'block';
      }

      if (shouldHideComingSoon()) {
        hideComingSoon();
      }

      return {
        hideComingSoon: hideComingSoon
      };
    }());
  ''')
}
