/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import groovy.util.Expando;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;

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
  }
}
