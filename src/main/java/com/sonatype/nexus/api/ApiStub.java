/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.api;

import groovy.util.Expando;

import java.io.IOException;
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
      return new NxrmClient();
    }
  }

  public static class NxrmClient
  {
    public List<Expando> getNxrmRepositories() throws IOException {
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
