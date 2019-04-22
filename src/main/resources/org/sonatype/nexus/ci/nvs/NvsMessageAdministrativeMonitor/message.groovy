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
package org.sonatype.nexus.ci.nvs.NvsMessageAdministrativeMonitor

import lib.FormTagLib

def f = namespace(FormTagLib)

def link = "https://www.sonatype.com/nvsforjenkins?utm_campaign=AP%20NVS%20for%20Jenkins&utm_source=AP%20Pages&utm_medium=AP%20&utm_term=April%202019&utm_content=AP"

div(class: "alert alert-info") {
  div(style: "float:right;") {
    f.form(method: "post", action: "${rootURL}/${my.url}/disable") {
      f.submit(value: "Dismiss")
    }
  }
  span("Do you have additional security vulnerabilities hiding in your build artifacts?")
  br()
  a(href: link, target: "_blank", "Learn more")
  span(" about enhancements to the Nexus Platform Plugin that will help you discover and resolve lurking problems.")
}
