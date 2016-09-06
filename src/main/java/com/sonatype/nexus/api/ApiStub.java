/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.api;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import groovy.util.Expando;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class ApiStub
{
  public static class NexusClientFactory
  {
    public static NxrmClient buildRmClient(String url, String credentialsId) throws URISyntaxException {
      NxrmClient client = new NxrmClient();
      client.url = url;
      client.credentials = CredentialsMatchers.firstOrNull(CredentialsProvider
          .lookupCredentials(StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
              URIRequirementBuilder.fromUri(url).build()), CredentialsMatchers.withId(credentialsId));

      client.uri = new URI(url + "/service/local/artifact/maven/content");
      client.host = new HttpHost(client.uri.getHost(), client.uri.getPort());

      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      AuthScope authScope = new AuthScope(client.uri.getHost(), client.uri.getPort());
      UsernamePasswordCredentials usernamePassword = new UsernamePasswordCredentials(client.credentials.getUsername(),
          client.credentials.getPassword().getPlainText());
      credentialsProvider.setCredentials(authScope, usernamePassword);
      BasicAuthCache authCache = new BasicAuthCache();
      BasicScheme authScheme = new BasicScheme();
      authCache.put(client.host, authScheme);

      client.context = HttpClientContext.create();
      client.context.setAuthCache(authCache);
      client.httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();

      return client;
    }
  }

  public static class NxrmClient
  {
    private String url;

    private StandardUsernamePasswordCredentials credentials;

    private URI uri;

    private HttpClient httpClient;

    private HttpHost host;

    private HttpClientContext context;

    public List<Expando> getNxrmRepositories() throws IOException {
      if (url == null || url.equals("http://fake.com")) {
        throw new IOException("FAIL");
      }

      LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(5);
      map.put("id", "releases");
      map.put("name", "Releases");
      map.put("repoType", "hosted");
      map.put("repoPolicy", "release");
      map.put("format", "maven2");
      return new ArrayList<>(Collections.singletonList(new Expando(map)));
    }

    public void uploadComponent(final String repositoryId, final MavenCoordinate mavenCoordinate,
                                final MavenFile mavenFile) throws IOException
    {
      if (url == null || url.equals("http://fake.com")) {
        throw new IOException("FAIL");
      }

      HttpPost post = new HttpPost(uri);
      FileBody artifact = new FileBody(mavenFile.getArtifact());
      String extension = StringUtils.isEmpty(mavenFile.getExtension()) ? mavenCoordinate.getPackaging() : mavenFile
          .getExtension();

      HttpEntity request = MultipartEntityBuilder.create()
          .addPart("r", new StringBody(repositoryId, ContentType.TEXT_PLAIN))
          .addPart("hasPom", new StringBody("false", ContentType.TEXT_PLAIN))
          .addPart("e", new StringBody(extension, ContentType.TEXT_PLAIN))
          .addPart("g", new StringBody(mavenCoordinate.getGroupId(), ContentType.TEXT_PLAIN))
          .addPart("a", new StringBody(mavenCoordinate.getArtifactId(), ContentType.TEXT_PLAIN))
          .addPart("v", new StringBody(mavenCoordinate.getVersion(), ContentType.TEXT_PLAIN))
          .addPart("p", new StringBody(mavenCoordinate.getPackaging(), ContentType.TEXT_PLAIN))
          .addPart("file", artifact).build();

      post.setEntity(request);

      try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(host, post, context)) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_CREATED) {
          // This needs to be flushed out but will wait for the API
          throw new IOException();
        }
      }
    }
  }

  public static class MavenCoordinate
  {
    private String groupId;

    private String artifactId;

    private String version;

    private String packaging;

    public String getGroupId() {
      return groupId;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public void setArtifactId(String artifactId) {
      this.artifactId = artifactId;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getPackaging() {
      return packaging;
    }

    public void setPackaging(String packaging) {
      this.packaging = packaging;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MavenCoordinate that = (MavenCoordinate) o;

      if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) {
        return false;
      }
      if (artifactId != null ? !artifactId.equals(that.artifactId) : that.artifactId != null) {
        return false;
      }
      if (version != null ? !version.equals(that.version) : that.version != null) {
        return false;
      }
      return packaging != null ? packaging.equals(that.packaging) : that.packaging == null;

    }

    @Override
    public int hashCode() {
      int result = groupId != null ? groupId.hashCode() : 0;
      result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
      result = 31 * result + (version != null ? version.hashCode() : 0);
      result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
      return result;
    }
  }

  public static class MavenFile
  {
    public File getArtifact() {
      return artifact;
    }

    public void setArtifact(File artifact) {
      this.artifact = artifact;
    }

    public String getExtension() {
      return extension;
    }

    public void setExtension(String extension) {
      this.extension = extension;
    }

    public String getClassifier() {
      return classifier;
    }

    public void setClassifier(String classifier) {
      this.classifier = classifier;
    }

    private File artifact;

    private String extension;

    private String classifier;

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MavenFile mavenFile = (MavenFile) o;

      if (artifact != null ? !artifact.equals(mavenFile.artifact) : mavenFile.artifact != null) {
        return false;
      }
      if (extension != null ? !extension.equals(mavenFile.extension) : mavenFile.extension != null) {
        return false;
      }
      return classifier != null ? classifier.equals(mavenFile.classifier) : mavenFile.classifier == null;

    }

    @Override
    public int hashCode() {
      int result = artifact != null ? artifact.hashCode() : 0;
      result = 31 * result + (extension != null ? extension.hashCode() : 0);
      result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
      return result;
    }
  }
}
