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
  println '############################################################'
  println ''
  def classesToFind = [
      "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
      "org.cyclonedx.generators.xml.AbstractBomXmlGenerator",
  ]
  for (className in classesToFind) {
    printPluginsLoadingClass(className)
  }
}

def printPluginsLoadingClass(className) {
  def clazzResource = className.replaceAll("\\.", "/") + ".class"
  println "Looking for: ${clazzResource}"
  println '-------------------------------------------------------------'
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
  println '############################################################'
  println ''
  Jenkins.instance.pluginManager.activePlugins.forEach { PluginWrapper plugin ->
    println "Found: ${plugin}"
    println '-------------------------------------------------------------'
    println "BackupVersion: ${plugin.getBackupVersion()}"
    println "Dependents: ${plugin.getDependents()}"
    println "Dependencies: ${plugin.getDependencies()}"
    println "RequiredCoreVersion: ${plugin.getRequiredCoreVersion()}"
    println "Version: ${plugin.getVersion()}"
    println "VersionNumber: ${plugin.getVersionNumber()}"
    println "DerivedDependencyErrors: ${plugin.getDerivedDependencyErrors()}"
    println ''
  }
}

/**
 * Prints information about packages loaded by each plugin installed on the Jenkins server
 */
def printClassLoaderInformationForPlugins() {
  println 'Looking for Plugins Class Loader Information'
  println '############################################################'
  println ''
  Jenkins.instance.pluginManager.activePlugins.forEach { PluginWrapper plugin ->
    println "Found ${plugin}"
    println '-------------------------------------------------------------'
    println "Packages: ${plugin.classLoader.getPackages()}"
    println "Classloader: ${plugin.classLoader}"
    println "Parent Classloader: ${plugin.classLoader.getParent()}"    
    println ''
  }
}

return
