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

A plugin for integrating Nexus Repository Manager and Nexus Lifecycle into a Jenkins job. Information about using the plugin can be found in [Nexus Platform Plugin for Jenkins](https://links.sonatype.com/products/nxiq/doc/nexus-platform-plugin-for-jenkins).

Please use the links below to find information about using the plugin with your desired software

 * [Nexus Repository Manager 2](https://links.sonatype.com/products/nxrm2/jenkins)
 * [Nexus Repository Manager 3](https://links.sonatype.com/products/nxrm3/docs/integrations/jenkins)
 * [Nexus Lifecycle/IQ](https://links.sonatype.com/products/nxiq/doc/nexus-platform-plugin-for-jenkins)

Changelog
=========
release-3.19.0-01 (December 08, 2023)
------------------------------------------------
- Java 11 is the minimum supported Java version
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release170(December2023)) for Nexus Lifecycle 1.170.0-01.

release-3.18.0-03 (November 02, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release169(October2023)) for Nexus Lifecycle 1.169.0-01.

3.17.518.v9cb_3ff833922 (September 08, 2023)
------------------------------------------------
- Rename the feature previously known as Policy Violation Grandfathering to [Legacy Violations](https://help.sonatype.com/iqserver/managing/policy-management/managing-legacy-violations).
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release167(September2023)) for Nexus Lifecycle 1.167.0-01.

3.17.514.va_6dfca_8a_f7a_c (September 01, 2023)
------------------------------------------------
- Add support for scanning SPDX version 2.3 files.
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release166(August2023)) for Nexus Lifecycle 1.166.0-01.

3.16.510.v4d23e22cf563 (August 01, 2023)
------------------------------------------------
- Add support for scanning Java class binaries produced by Java 19 and 20.

3.16.508.vfc408b_9601f0 (July 20, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release165(July2023)) for Nexus Lifecycle 1.165.0-01.

3.16.503.vb_a_7b_10f1c4cf (July 05, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release164(June2023)) for Nexus Lifecycle 1.164.0-01.

3.16.501.ve3d6b_58f1d37 (June 16, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release163(June2023)) for Nexus Lifecycle 1.163.0-01.

3.16.497.vd8491dd15a_8d (June 13, 2023)
------------------------------------------------
- Add full support for Java 17
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release162(June2023)) for Nexus Lifecycle 1.162.0-01.

3.16.491.v77a_2f8921c88 (May 12, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release161(May2023)) for Nexus Lifecycle 1.161.0-01.

3.16.489.v7cf06846a_c96 (April 20, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release160(April2023)) for Nexus Lifecycle 1.160.0-01.

3.16.487.v5d4d3b_6942ee (April 06, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release159(April2023)) for Nexus Lifecycle 1.159.0-01.

3.16.485.ve2c3a_17ec407 (March 22, 2023)
------------------------------------------------
- For remote image scanning, environmental variables NEXUS_CONTAINER_IMAGE_REGISTRY_USER and NEXUS_CONTAINER_IMAGE_REGISTRY_PASSWORD are not required and are now optional for public images. 
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release158(March2023)) for Nexus Lifecycle 1.158.0-01.

3.16.481.ved9f5106e132 (February 27, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release156(February2023)) for Nexus Lifecycle 1.156.0-01.

3.16.478.v41ee37380162 (February 13, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release155(February2023)) for Nexus Lifecycle 1.155.0-01.

3.16.476.v410d6968f400 (January 27, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release153(January2023)) for Nexus Lifecycle 1.153.0-01.

3.16.474.vb_0cdf4908780 (January 16, 2023)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release152(January2023)) for Nexus Lifecycle 1.152.0-01.

3.16.471.v2dcf088efb_7f (December 14, 2022)
------------------------------------------------
- Removing support for scanning IaC targets
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release151(December2022)) for Nexus Lifecycle 1.151.0-01.

3.16.465.ve8709b_fa_df42 (December 01, 2022)
------------------------------------------------
- Scanning local images does not require providing environmental variables.
- To scan remote images, the user will now have to provide only these variables:
  - NEXUS_CONTAINER_IMAGE_REGISTRY_USER
  - NEXUS_CONTAINER_IMAGE_REGISTRY_PASSWORD
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release150(November2022)) for Nexus Lifecycle 1.150.0-01.

3.16.459.vcdf273b_29f8c (November 17, 2022)
------------------------------------------------
- Fix for an edge case where the policy evaluation pipeline stage is marked UNSTABLE even though no policy violations are present.
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release149(November2022)) for Nexus Lifecycle 1.149.0-01.

3.16.455.vd5654e1c14b_a_ (October 26, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release147(October2022)) for Nexus Lifecycle 1.147.0-01.

3.16.453.v39a_b_a_0401562 (October 20, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release146(October2022)) for Nexus Lifecycle 1.146.0-01.

3.16.449.v50228c7ca_222 (October 06, 2022)
------------------------------------------------
- Policy evaluation stage is marked according to policy actions.
- Fix dependency conflict with certain versions of the credentials plugin.
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release145(October2022)) for Nexus Lifecycle 1.145.0-01.

3.16.444.v52b_e5e2db_503 (September 22, 2022)
------------------------------------------------
- Add the organization ID parameter, used for automatic IQ apps
- Fixing Nexus IQ Build Report to properly show the icon for "notify" actions
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release144(September2022)) for Nexus Lifecycle 1.144.0-05.

3.15.438.vf87a_0dc45166 (September 07, 2022)
------------------------------------------------
* Using lightweight Nexus Java API to reduce complexity on class loading for the plugin
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release143(September2022)) for Nexus Lifecycle 1.143.0-01.

3.14.431.v37ca_dc788b_b_1 (July 28, 2022)
------------------------------------------------
* Improving summary message for policy evaluations
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release142(July2022)) for Nexus Lifecycle 1.142.0-02.

3.14.424.v8290b_b_ec62cb_ (July 07, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release141(June2022)) for Nexus Lifecycle 1.141.0-01.

3.14.418.v7a_687b_6a_4c1d (June 16, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release139(June2022)) for Nexus Lifecycle 1.139.0-01.

3.14.415.v4605773547f3 (June 01, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release138(May2022)) for Nexus Lifecycle 1.138.0-01.

3.14.412.v8021dc9cc4ef (May 19, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release137(May2022)) for Nexus Lifecycle 1.137.0-05.

3.14.407.v9d113b_445204 (April 29, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release136(April2022)) for Nexus Lifecycle 1.136.0-01.
- Add support for scanning Java class binaries produced by Java 18.

3.14.405.v74e19a_0b_1a_1a_ (March 28, 2022)
------------------------------------------------
- Fix a `IllegalAccessError` that prevents the plugin to run properly in certain cases.

3.14.403.v07c2f1f96d60 (March 24, 2022)
------------------------------------------------
- Fix a `NoSuchMethodError` exception that prevents the plugin to run in certain cases.
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release135(March2022)) for Nexus Lifecycle 1.135.0-01.

3.14.401.v1311ea_023ce5 (March 15, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release134(March2022)) for Nexus Lifecycle 1.134.0-02.

3.13.20220304-155321.e7fcac5 (March 04, 2022)
------------------------------------------------
- Provide [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release133(March2022)) for Nexus Lifecycle 1.133.0-02.

3.13.20220201-143240.3d657a5 (February 01, 2022)
------------------------------------------------
- Reduce logging on INFO level

3.13.20220124-164651.0b71b72 (January 24, 2022)
------------------------------------------------
- Provide the [latest features](https://help.sonatype.com/iqserver/product-information/release-notes#ReleaseNotes-Release132(January2022)) for Nexus Lifecycle 132
- Bug Fix for False Positives in Image Scans

3.13.20220121-121645.a0ca2c5 (January 21, 2022)
------------------------------------------------
- Added support for scanning IaC targets

3.13.20211220-113820.efa5a1c (December 20th, 2021)
------------------------------------------------
- Conda Matching Improvements
- Cran and Cargo Matching Improvements

3.13.20211207-082721.a97491c (December 7th, 2021)
------------------------------------------------
- Updated the min Jenkins version required to 2.249.1
- Removed obsolete dependencies

3.13.20211117-154915.1ea721a (November 18th, 2021)
------------------------------------------------
- Added support for multiple Nexus IQ Servers.

3.12.20211110-124942.5dc6cea (November 11th, 2021)
------------------------------------------------
- Fixed java.lang.NoClassDefFoundError: io/jenkins/cli/shaded/org/xml/sax/ContentHandler.

3.12.20211019-085324.d8da475 (October 21th, 2021)
------------------------------------------------
- Added support for scanning Java class binaries produced by Java 17.

3.11.20210920-123737.0869e33 (September 20, 2021)
-------------------------------------------------
- Added support for using environment variables and credentials for required values for container scanning
- Made default mount folder for nexus container analysis workspace temp folder

3.11.20210915-164919.37a20aa (September 16, 2021)
-------------------------------------------------
- Bug fixes
- NPM manifest file scans now include dependency information and can identify InnerSource components

3.11.20210824-103237.60c1db0 (August 25, 2021)
------------------------------------------------
- Made mount folder for nexus container analysis customisable
- Made default mount folder /tmp for nexus container analysis
- Improvements in log statements for nexus container analysis

3.11.20210811-095455.fdf8fec (August 11, 2021)
------------------------------------------------
- Bug fixes

3.11.20210729-123253.8df0e2b (July 30, 2021)
------------------------------------------------
- Handle yarn v2 files
- Exclude package-lock.json in favour of npm-shrinkwrap.json
- Bug fixes

3.11.20210716-143001.0533f8f (July 16, 2021)
------------------------------------------------
- Add change log for 3.11.20210716-075132.3b66565 (July 16, 2021)

3.11.20210716-075132.3b66565 (July 16, 2021)
------------------------------------------------
- Add support for nexus container analysis
- Make build unstable on scan error

3.11.20210621-093929.6318134 (June 21st, 2021)
------------------------------------------------
- Delete temp files from scan after eval
- Send licensed features into the scanner
- Fix runtime error due to stax2 conflict
- Add jenkins version to user agent

3.11.20210420-142258.bdfc332 (April 20th, 2021)
------------------------------------------------
- Added support for scanning Java class binaries produced by Java 16.
- Fix XStream parser error when scanning nuget manifests

3.11.20210323-112924.daaeac7 (March 24th, 2021)
------------------------------------------------
- Fix a regression in configuring the Policy Evaluation task in the UI.

3.11.20210308-082521.0d183ff (March 8th, 2021)
------------------------------------------------
- Added scanning and application/package analysis support for Java using a pom.xml or build.gradle file.

3.11.20210301-084816.bd7c972 (March 1, 2021)
------------------------------------------------
- Added a Global Configuration option to remove direct IQ reporting of policy violations from Jenkins.

3.10.20210222-102732.7875f67 (February 23rd, 2021)
------------------------------------------------
- Update the resultant <dependencies> structure to include the nested dependencies to form a dependency tree when scanning a module.xml file.
- Added scanning and application/package analysis support for the following ecosystems:
    - NPM using files : yarn.lock, pnpm-lock.yaml, package-lock.json, npm-shrinkwrap.json
    - Nuget using packages.config file or .csproj files

3.10.20201208-151941.d953318 (December 8th, 2020)
------------------------------------------------
- Added support for running the plugin with Java 11 and 14.
- Added support for scanning Java class binaries produced by Java 14 and 15.

3.9.20201109-154552.99ba8b9 (November 9th, 2020)
------------------------------------------------
- Added flag to enable debug logging.

3.9.20200722-164144.e3a1be0 (July 22nd, 2020)
------------------------------------------------
- Added scanning and application/package analysis support for Conan using a conaninfo.txt file (in addition to the files conanfile.txt and conanfile.py).

3.9.20200716-164408.7b4a45f (July 16th, 2020)
------------------------------------------------
- Added scanning and application/package analysis support for Golang using a go.list file (in addition to the file go.sum).

3.9.20200623-110149.2e546a0 (June 23rd, 2020)
------------------------------------------------
- Added scanning and application/package analysis support for the following ecosystems:
  - Alpine
  - Conda
  - Debian
  - Drupal
  - R (Cran)
  - Rust (Cargo)
  - Swift (Cocoapods)
  - Yum
- Use policy violation counts instead of component counts in the policy evaluation summary
- Fixed an issue with y-axis labels on the new trend graph

3.8.20200204-101107.d1d344b (February 6th, 2020)
------------------------------------------------
- Fix to ensure that all Nexus IQ for SCM logging goes to the build log instead of the server log

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
    Please see [Nexus Platform Plugin for Jenkins](https://help.sonatype.com/iqserver/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins) for more details.

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

-   The pipeline step has always returned a model for the evaluation containing information about the [results](https://help.sonatype.com/iqserver/integrations/nexus-and-continuous-integration/nexus-platform-plugin-for-jenkins#NexusPlatformPluginforJenkins-ReturnValue). The ApplicationPolicyEvaluation will no longer include a boolean for reevaluation therefore calls to get or set this will fail. The Jenkins pipeline has never supported reevaluation and this boolean has always returned false. For simplification, it has been removed.

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
