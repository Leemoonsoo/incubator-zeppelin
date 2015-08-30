/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.dep;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;
import org.sonatype.aether.util.filter.PatternExclusionsDependencyFilter;


/**
 * Deps resolver.
 * Add new dependencies from mvn repo (at runetime) to Zeppelin.
 *
 */
public class DependencyResolver {
  Logger logger = LoggerFactory.getLogger(DependencyResolver.class);
  private RepositorySystem system = Booter.newRepositorySystem();
  private List<RemoteRepository> repos = new LinkedList<RemoteRepository>();
  private RepositorySystemSession session;

  private final String[] exclusions = new String[] {"org.apache.zeppelin:zeppelin-zengine",
                                                    "org.apache.zeppelin:zeppelin-interpreter",
                                                    "org.apache.zeppelin:zeppelin-server"};

  public DependencyResolver(String localRepoPath) {
    session = Booter.newRepositorySystemSession(system, localRepoPath);
    repos.add(Booter.newCentralRepository()); // add maven central
    repos.add(new RemoteRepository("local", "default", "file://"
        + System.getProperty("user.home") + "/.m2/repository"));
  }

  public void addRepo(String id, String url, boolean snapshot) {
    synchronized (repos) {
      delRepo(id);
      RemoteRepository rr = new RemoteRepository(id, "default", url);
      rr.setPolicy(snapshot, null);
      repos.add(rr);
    }
  }

  public RemoteRepository delRepo(String id) {
    synchronized (repos) {
      Iterator<RemoteRepository> it = repos.iterator();
      if (it.hasNext()) {
        RemoteRepository repo = it.next();
        if (repo.getId().equals(id)) {
          it.remove();
          return repo;
        }
      }
    }
    return null;
  }


  public List<String> load(String artifact) throws Exception {
    return load(artifact, new LinkedList<String>());
  }

  public List<String> load(String artifact, Collection<String> excludes) throws Exception {
    if (StringUtils.isBlank(artifact)) {
      // Should throw here
      throw new RuntimeException("Invalid artifact to load");
    }

    // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
    int numSplits = artifact.split(":").length;
    if (numSplits >= 3 && numSplits <= 6) {
      return loadFromMvn(artifact, excludes);
    } else {
      LinkedList<String> libs = new LinkedList<String>();
      libs.add(artifact);
      return libs;
    }
  }


  private List<String> loadFromMvn(String artifact, Collection<String> excludes) throws Exception {
    List<String> loadedLibs = new LinkedList<String>();
    Collection<String> allExclusions = new LinkedList<String>();
    allExclusions.addAll(excludes);
    allExclusions.addAll(Arrays.asList(exclusions));

    List<ArtifactResult> listOfArtifact;
    listOfArtifact = getArtifactsWithDep(artifact, allExclusions);

    Iterator<ArtifactResult> it = listOfArtifact.iterator();
    while (it.hasNext()) {
      Artifact a = it.next().getArtifact();
      String gav = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
      for (String exclude : allExclusions) {
        if (gav.startsWith(exclude)) {
          it.remove();
          break;
        }
      }
    }

    List<URL> newClassPathList = new LinkedList<URL>();
    List<File> files = new LinkedList<File>();
    for (ArtifactResult artifactResult : listOfArtifact) {
      logger.info("Load " + artifactResult.getArtifact().getGroupId() + ":"
          + artifactResult.getArtifact().getArtifactId() + ":"
          + artifactResult.getArtifact().getVersion());
      newClassPathList.add(artifactResult.getArtifact().getFile().toURI().toURL());
      files.add(artifactResult.getArtifact().getFile());
      loadedLibs.add(artifactResult.getArtifact().getGroupId() + ":"
          + artifactResult.getArtifact().getArtifactId() + ":"
          + artifactResult.getArtifact().getVersion());
    }

    return loadedLibs;
  }

  /**
   *
   * @param dependency
   * @param excludes list of pattern can either be of the form groupId:artifactId
   * @return
   * @throws Exception
   */
  public List<ArtifactResult> getArtifactsWithDep(String dependency,
      Collection<String> excludes) throws Exception {
    Artifact artifact = new DefaultArtifact(dependency);
    DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE );
    PatternExclusionsDependencyFilter exclusionFilter =
        new PatternExclusionsDependencyFilter(excludes);

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));

    synchronized (repos) {
      for (RemoteRepository repo : repos) {
        collectRequest.addRepository(repo);
      }
    }
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest,
        DependencyFilterUtils.andFilter(exclusionFilter, classpathFlter));
    return system.resolveDependencies(session, dependencyRequest).getArtifactResults();
  }
}
