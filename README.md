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
# Nexus Platform Plugin

A plugin for integrating Nexus Repository Manager and Nexus Lifecycle into a Jenkins job. Information about using the plugin can be found in [Nexus Platform Plugin for Jenkins](https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins).

Please use the links below to find information about using the plugin with your desired software

 * [Nexus Repository Manager 2](https://help.sonatype.com/display/NXRM2/Jenkins)
 * [Nexus Repository Manager 3](https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins#NexusPlatformPluginforJenkins-RepositoryManager3Integration)
 * [Nexus Lifecycle/IQ](https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins#NexusPlatformPluginforJenkins-NexusIQServerIntegration)

Changelog
=========
3.8.20191216-154521.a7bf2be (December 18th, 2019)
--------------------------------------------------
- Fix additional marshalling issue with new trend graph

3.8.20191213-085900.c28ded4 (December 13th, 2019)
--------------------------------------------------
- Fix marshalling issue with new trend graph
- Fix issue with y-axis number on new trend graph

3.8.20191204-084645.a4bff16 (December 4th, 2019)
--------------------------------------------------
- Add Nexus IQ Build Report which shows details for warn/fail vulnerabilities
- Support slave nodes for automatic repository URL discovery for usage with [Nexus IQ for SCM](https://help.sonatype.com/integrations/nexus-iq-for-scm)

3.8.20191127-111424.5d61f82 (November 27th, 2019)
--------------------------------------------------
- Add trend graph to a Pipeline, which depicts the information about the last 5 builds with critical, severe and moderate violation numbers
- [Support to scan and evaluate Clair identified container dependencies](https://help.sonatype.com/iqserver/analysis/clair-application-analysis)
- [Support to scan and evaluate identified dependencies from a CycloneDX SBOM file](https://help.sonatype.com/iqserver/analysis/cyclonedx-application-analysis)

3.8.20190920-091853.5b0aa4e (September 20th, 2019)
--------------------------------------------------

-   Support for automatically deducing the repository URL for usage with [Nexus IQ for SCM](https://help.sonatype.com/integrations/nexus-iq-for-scm)

3.7.20190823-091836.9f85050 (August 23rd, 2019)
-----------------------------------------------

-   Support for automatically deducing git commit hash for usage with [Nexus IQ for SCM](https://help.sonatype.com/integrations/nexus-iq-for-scm)

3.6.20190722-122200.83d1447 (July 22nd, 2019)
---------------------------------------------

### BREAKING CHANGES

-   Nexus IQ 1.69 or newer is a required upgrade to use the Nexus Platform Plugin
-   Support for Scanning Go Modules
-   Mitigate IQ Server Client Timeouts

3.5.20190425-152158.c63841b (April 25th, 2019)
----------------------------------------------

-   Add messages about Nexus Vulnerability Scanner to the plugin
-   Add ability to provide custom/advanced properties to IQ scanner

3.5.20190422-102004.71358d2 (April 22nd, 2019)
----------------------------------------------

-   Fix for environmental variables not getting resolved in the tags field

3.5.20190313-114450.3bfee7f (March 13th, 2019)
----------------------------------------------

-   Support for Java 12 IQ evaluations

3.5.20190215-104018.385de7e (February 18th, 2019)
-------------------------------------------------

-   Support for Scanning Python Wheel Packages

3.4.20190116-104331.e820fec (January 16th, 2019)
------------------------------------------------

-   Support for Java 10, 11 IQ evaluations
-   Support for Python coordinate detection via requirements.txt files

3.3.20190108-134259.b70ae43 (January 8th, 2019)
-----------------------------------------------

-   Support for multiple policy evaluations per Jenkins job
-   Added application name and IQ stage to the entries in the build results
-   Renamed the "Application Composition Report" to "Nexus IQ Policy Evaluation"

3.3.20181207-134824.d240aa3 (December 12, 2018)
-----------------------------------------------

-   [Fixed] Could not connect to Nexus Repository servers exposed over HTTPS
-   [Fixed] Proxy settings were not respected when verifying connection to Nexus Repository

3.3.20181129-003933.7701a25 (November 29, 2018)
-----------------------------------------------

-   [Fixed] IQ application list incorrect for jobs configured to use job specific credentials

3.3.20181102-112614.a65c3f1 (November 2, 2018)
----------------------------------------------

-   [Fixed] Environment variables weren't expanded for manual application IDs

3.3.20181025-134249.614c5f4 (October 25, 2018)
----------------------------------------------

-   [Fixed] When configuring the 'Invoke Nexus Policy Evaluation' build step, the 'module excludes' field is not persisted on save.
-   [Fixed] Jenkins Platform Plugin unable to determine Nexus Repository Manager version using Server URL with trailing slash
-   [Fixed] Jenkins plugin fails requests when Nexus is not at base context path
-   Add link to plugin documentation for NXRM3 to readme

3.3.20180912-170211.be90294 (September 12, 2018)
------------------------------------------------

-   The plugin will now emit a warning when the scanner encounters an invalid JAR file:\
    "[WARN] Could not open some.jar as an archive. Will scan it as regular file."

3.3.20180830-142202.6bdf614 (August 30, 2018)
---------------------------------------------

### BREAKING CHANGES

-   Nexus IQ 1.50 or newer is a required upgrade to use the Nexus Platform Plugin
-   Support for Nexus IQ [Policy Violation Grandfathering](https://help.sonatype.com/iqserver/policy-violation-grandfathering).
-   Fixed snippet generation.

3.3.20180801-112343.4970c8a (August 1, 2018)
--------------------------------------------

-   New build step available for tag association
-   Move components using NXRM3 search criteria from Pipeline

3.2.20180724-142843.2f5144d (July 24, 2018)
-------------------------------------------

-   Added support of Nexus Repository Manager 3.13.0-01 servers for Maven component uploads, and new staging features (for Pro versions): tags, move, and delete.\
    Please see [Nexus Platform Plugin for Jenkins](https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins) for more details.

3.1.20180702-132131.f6b4592 (July 2, 2018)
------------------------------------------

-   Fixes for recording of component occurrences

3.1.20180605-140134.c2e96c4 (June 5, 2018)
------------------------------------------

-   Log additions for [automatic application creation](https://help.sonatype.com/iqserver/organization-and-application-management/managing-automatic-applications)

3.0.20180531-100044.36b733a (May 31, 2018)
------------------------------------------

-   UI fixes for chiclet style on older versions of Jenkins

3.0.20180425-130011.728733c (April 25, 2018)
--------------------------------------------

### BREAKING CHANGES

-   Nexus IQ 1.47 or newer is a required upgrade to use the Nexus Platform Plugin
-   Support for Nexus IQ [automatic application creation](https://help.sonatype.com/iqserver/organization-and-application-management/managing-automatic-applications)

3.0.20180214-134325.e135900 (February 14, 2018)
-----------------------------------------------

### BREAKING CHANGES

-   Pipeline jobs using the plugin will now fail during execution if a policy action is set to fail the build. This is different from previous behavior which would set the build result to failure but allow the build to continue. This is adopting standard practice for Jenkins pipeline plugins and allows more visibility into what has failed and why. Pipelines that require continuation of the build will have to surround the plugin step with try catch, where the evaluation information is now wrapped in the exception argument.

-   The pipeline step has always returned a model for the evaluation containing information about the [results](https://help.sonatype.com/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins#NexusPlatformPluginforJenkins-ReturnValuefromPipelineBuild). The ApplicationPolicyEvaluation will no longer include a boolean for reevaluation therefore calls to get or set this will fail. The Jenkins pipeline has never supported reevaluation and this boolean has always returned false. For simplification, it has been removed.

### CHANGES

-   Module.xml evaluation support. The Nexus Platform Plugin for Jenkins now supports policy evaluations against results generated by the clm-maven-plugin index goal. The new plugin will scan module.xml files available in '**/sonatype-clm/module.xml', '**/nexus-iq/module.xml' and will support module exclude patterns to exclude these files if desired.
-   Fix for directory structure of JavaScript files scanned by the plugin
-   No longer requires optional parameters to be declared in declarative pipelines
-   All users can now select credentials for jobs as long as they have the appropriate permissions to configure the job and view the credentials

1.6.20180123-131927.f506018 (January 23, 2018)
----------------------------------------------

-   Whitelist updates to support [JEP-200](https://jenkins.io/blog/2018/01/13/jep-200/)

1.5.20171121-095817.c18bf4f (November 21, 2017)
-----------------------------------------------

-   Support for Java 9 IQ evaluations

1.4.20170929-233916.04479e6 (September 29, 2017)
------------------------------------------------

-   Update upstream dependencies to consume latest IQ server Application Evaluation result
-   Fix for throwing serializable exception upon client exception

1.3.20170728-122322.902d97e (July 28, 2017)
-------------------------------------------

-   Support for Docker image evaluations

1.2.20170627-094410.1e61c09 (June 27th, 2017)
---------------------------------------------

-   Support for credentials in Folder stores
-   Support for Certificate credentials through the [Credentials Plugin](https://wiki.jenkins.io/display/JENKINS/Credentials+Plugin)

1.2.20170428-142845.cb63c9e (April 28th, 2017)
----------------------------------------------

-   Support for Nexus Publish when remote agent is used for build.

1.2.20170417-120258.3e88a58 (April 17th, 2017)
----------------------------------------------

-   Fix for connection pool saturation when publishing many components.

1.2.20170404-163441.794de4c (April 4th, 2017)
---------------------------------------------

-   Initial release to the Jenkins Update Center.


LICENSE
=========

    Copyright (c) 2016-present Sonatype, Inc. All rights reserved.

    This program is licensed to you under the Apache License Version 2.0,
    and you may not use this file except in compliance with the Apache License Version 2.0.
    You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.

    Unless required by applicable law or agreed to in writing,
    software distributed under the Apache License Version 2.0 is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
