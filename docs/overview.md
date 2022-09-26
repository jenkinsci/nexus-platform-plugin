<!--

    Copyright (c) 2016-present Sonatype, Inc. All rights reserved.

    This program is licensed to you under the Apache License Version 2.0,
    and you may not use this file except in compliance with the Apache License Version 2.0.
    You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.

    Unless required by applicable law or agreed to in writing,
    software distributed under the Apache License Version 2.0 is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.

-->

# Document Purpose

This page provides high-level technical information regarding the **Nexus Platform Plugin for Jenkins**.


## Product Overview

For an overview of the product and its features see the plugin's public [help page][1]. 
Also, you can check out the [README.md](../README.md).


## High-Level Technical Description

TBD


## Local Development

### Build

To build the project run:
```bash
mvn clean install
```

### Run and Debug

To start the plugin in Jenkins run:
```bash
mvn hpi:run
```

You can also create a Maven-based run configuration in IDEA and execute it in run or debug mode.

## Development Notes

### Stax2 implementation conflict

**Problem**:

Conflicting dependencies in Jenkins:

Jenkins provided jar:
```
[INFO] org.sonatype.nexus.ci:nexus-jenkins-plugin:hpi:3.11-SNAPSHOT
[INFO] \- org.jenkins-ci.main:jenkins-core:jar:2.7:provided
[INFO]    \- org.codehaus.woodstox:wstx-asl:jar:3.2.9:provided
```

Bundled with nexus-java-api v3.38:
```
[INFO] com.sonatype.insight.scan:insight-scanner-archive:jar:2.29.1-SNAPSHOT
[INFO] \- org.cyclonedx:cyclonedx-core-java:jar:4.1.2:compile
[INFO]    \- com.fasterxml.jackson.dataformat:jackson-dataformat-xml:jar:2.12.3:compile
[INFO]       \- com.fasterxml.woodstox:woodstox-core:jar:6.2.4:compile
```

Both jars contain similar classes with the same qualified names. The provided jar (which contains older classes) has 
priority in the class loader, but the bundled one (with newer classes) is needed by the plugin.

**Solution (old)**:

The conflicting classes and their dependencies are relocated in nexus-java-api, creating an isolated space for the 
plugin to execute all Stax2 operations. For details see: https://github.com/sonatype/nexus-java-api/pull/163/files

**Note**: This solution is no longer working as of Mar 22. 2022. We should still keep it here for reference.

**Solution (new)**:

By using the `<maskClasses>` feature of the `maven-hpi-plugin` we can mask certain packages from Jenkins-core. 
That makes them unavailable to the plugin, forcing the plugin to use its own bundled packages.

E.g.
```xml
<plugin>
    <groupId>org.jenkins-ci.tools</groupId>
    <artifactId>maven-hpi-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <maskClasses>
            com.fasterxml.jackson.
            org.cyclonedx.
            org.codehaus.stax2.
            com.ctc.wstx.
        </maskClasses>
    </configuration>
</plugin>
```

### Stax's implementation provided by Jenkins does not work with multiple service implementations

**Problem**:

Stax expects that implementations are provided for several service interfaces e.g. `hidden.javax.xml.stream.XMLOutputFactory`. 
In case multiple implementations are provided for the same service, the first one found is always used. 

Jenkins provides implementations for all the Stax services, and those implementations will be used by Jenkins and 
plugins, because Jenkins-core is always loaded first. The current mechanism prevents plugins to provide and use
different/newer Stax/Stax2 implementations.

**Solution**:

Plugins that require newer Stax/Stax2 functionality have to provide their own relocated Stax/Stax2 stack, to not 
interfere with the Stax/Stax2 classes used by Jenkins.


### Some (old) libraries assume there is only one class loader

**Problem**:

Jenkins uses a hierarchy of class loaders as shown below (for more details go to: [Class loading in Jenkins][2]):
```
□ Java Platform
 ↖
  □ “application classpath” (servlet container): java -jar jenkins.war
   ↖
    □ Jenkins core: jenkins.war!/WEB-INF/lib/*.jar
     ↖
      □ plugin A: $JENKINS_HOME/plugins/a.jpi!/WEB-INF/lib/*.jar
       ↖                                                             ↖
        □ plugin C: $JENKINS_HOME/plugins/c.jpi!/WEB-INF/lib/*.jar  ← □ UberClassLoader
     ↖ ↙                                                             ↙
      □ plugin B: $JENKINS_HOME/plugins/b.jpi!/WEB-INF/lib/*.jar
      ⋮
```

Some old java libraries (e.g. stax-api) assume the only one class loader exists, retrieve it using 
`Thread.currentThread().getContextClassLoader()` and use it exclusively. 

That class loader corresponds to the second entry in the above hierarchy, and it has access only to the 
jenkins-core packages (bundled in _jenkins.war_), but it cannot access any of the plugins provided classes.  

**Solution**:

Make sure the plugin class loader is used as a context class loader by the current thread.

E.g. in a class where you experience unexpected `ClassNotFoundExceptions` add:
```java  
  // get plugin's ClassLoader
  ClassLoader classLoader = this.class.classLoader
  // set context ClassLoader for current thread
  Thread.currentThread().setContextClassLoader(classLoader)
```


## References

### Jenkins Plugin Development

- [Dependencies and class loading][2]


[1]: https://help.sonatype.com/iqserver/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins
[2]: https://www.jenkins.io/doc/developer/plugin-development/dependencies-and-class-loading/
