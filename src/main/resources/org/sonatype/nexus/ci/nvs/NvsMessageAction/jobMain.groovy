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

import lib.JenkinsTagLib

def t = namespace(JenkinsTagLib)

def link = "https://www.sonatype.com/nvsforjenkins?utm_campaign=jenkins%20for%20nvs&utm_source=Project%20Pages&utm_medium=Project%20Pages&utm_term=April%202019&utm_content=PP"

style('''
  #nexus-plugin-nvs-message {
    display: none;
    padding: 1em 20px 0 1em;
    position: relative;
  }
  #nexus-plugin-nvs-message img {
    margin-right: 0 !important;
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
    span("Sonatype is making it even easier to ensure your applications are high quality.")
    br()
    a(href: link, target: "_blank", "Learn more")
    span(" about what's coming to the Nexus Platform Plugin.")
    div() {
      a(class: "nexus-plugin-nvs-hide-link", href: "#", onclick: "NexusPluginNVS.hideNvsMessage(); return false;",
          title: "close", "x")
    }
  }
}

table(id: "nexus-plugin-nvs-message") {
  t.summary(icon: "/plugin/nexus-jenkins-plugin/images/96x96/sonatype-logo.png", nvsMessage)
}

script {
  raw('''
    var NexusPluginNVS = (function() {
      'use strict';

      var cookieName = '_nexus_plugin_nvs_message';
      var cookie = cookieName + '=true; path=/; expires=Tue, 19 Jan 2038 03:14:07 GMT';  // max date
      var nvsMessage = document.getElementById('nexus-plugin-nvs-message');

      function shouldShowNvsMessage() {
        return document.cookie.indexOf(cookieName) === -1;
      }

      if (shouldShowNvsMessage()) {
        nvsMessage.style.display = 'table';
      }

      return {
        hideNvsMessage: function() {
          nvsMessage.style.display = 'none';
          document.cookie = cookie;
        }
      };
    })();
  ''')
}
