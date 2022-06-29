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

/**
 * This script is intended for support purposes only. The script do not get any sensitive information, it only gets
 * information about the installed plugins on the Jenkins server, the packages loaded by each plugin, and information
 * about how some classes are loaded by Jenkins.
 *
 * To run the script, you will need to go to the Script Console on the Jenkins server, paste the script, and hit the
 * "Run" button. Here you have more information:
 * ---> https://www.jenkins.io/doc/book/managing/script-console/
 */

// Print plugins loading classes
printPluginsLoadingClasses()

// Prints plugins information
printPluginsInformation()

// Prints plugins class loaders information
printClassLoaderInformationForPlugins()

/**
 * Prints information about how Jenkins is loading some classes
 */
def printPluginsLoadingClasses() {
  println 'Looking for Plugins Loading Classes'
  println('#' * 80)
  println ''
  def classesToFind = [
      'com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl',
      'org.cyclonedx.generators.xml.AbstractBomXmlGenerator',
  ]
  for (className in classesToFind) {
    printPluginsLoadingClass(className)
  }
}

def printPluginsLoadingClass(className) {
  def clazzResource = className.replaceAll("\\.", "/") + ".class"
  println "Looking for: ${clazzResource}"
  println('-' * 80)
  def list = []
  Jenkins.instance.pluginManager.activePlugins.forEach { PluginWrapper plugin ->
    def c = plugin.classLoader.getResources(clazzResource)
    if (c.hasMoreElements()) {
      list.add(plugin.toString())
    }
  }
  println "Class found in: ${list}"
  println ''
}

/**
 * Prints information about installed plugins on Jenkins server
 */
def printPluginsInformation() {
  println 'Looking for Plugins Information'
  println('#' * 80)
  println ''
  Jenkins.instance.pluginManager.activePlugins.forEach { PluginWrapper plugin ->
    println "Found: ${plugin}"
    println('-' * 80)
    println """
        BackupVersion: ${plugin.getBackupVersion()}
        Dependents: ${plugin.getDependents()}
        Dependencies: ${plugin.getDependencies()}
        RequiredCoreVersion: ${plugin.getRequiredCoreVersion()}
        Version: ${plugin.getVersion()}
        VersionNumber: ${plugin.getVersionNumber()}
        DerivedDependencyErrors: ${plugin.getDerivedDependencyErrors()}\n
    """.stripIndent()
  }
}

/**
 * Prints information about packages loaded by each plugin installed on the Jenkins server
 */
def printClassLoaderInformationForPlugins() {
  println 'Looking for Plugins Class Loader Information'
  println('#' * 80)
  println ''
  Jenkins.instance.pluginManager.activePlugins.forEach { PluginWrapper plugin ->
    println "Found ${plugin}"
    println('-' * 80)
    println """
        Packages: ${plugin.classLoader.getPackages()}
        Classloader: ${plugin.classLoader}
        Parent Classloader: ${plugin.classLoader.getParent()}\n
    """.stripIndent()
  }
}

return
