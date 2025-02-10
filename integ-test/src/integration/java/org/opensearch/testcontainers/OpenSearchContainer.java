/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * OpenSearchContainer Implementation.
 */
public class OpenSearchContainer extends GenericContainer<OpenSearchContainer> {

  private static final int DEFAULT_PORT = 9200;

  private static final String DEFAULT_IMAGE_NAME = "opensearchproject/opensearch:2.6.0";

  public OpenSearchContainer() {
    this(DockerImageName.parse(DEFAULT_IMAGE_NAME));
  }

  public OpenSearchContainer(String dockerImageName) {
    this(DockerImageName.parse(dockerImageName));
  }

  /**
   * Default OpenSearch Container.
   * Single node.
   * Disable security,
   */
  public OpenSearchContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);

    withExposedPorts(DEFAULT_PORT);
    withEnv("discovery.type", "single-node");
    withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true");
    withEnv("DISABLE_SECURITY_PLUGIN", "true");
    waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));
    withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/arm64"));
  }

  public int port() {
    return getMappedPort(DEFAULT_PORT);
  }

  private static final String OPENSEARCH_IMAGE = "opensearchproject/opensearch:2.6.0";
  private static final int OPENSEARCH_PORT = 9200;

  public static void main(String[] args) {
    String platform = System.getProperty("os.arch");
    System.out.println("Detected system architecture: " + platform);

    // Ensure the correct image is used
    DockerImageName imageName = DockerImageName.parse(OPENSEARCH_IMAGE);

    GenericContainer<?> openSearchContainer = new GenericContainer<>(imageName)
        .withExposedPorts(OPENSEARCH_PORT)
        .withEnv("discovery.type", "single-node")
        .withEnv("DISABLE_SECURITY_PLUGIN", "true")
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1)))
        .withReuse(true) // Allow reuse to prevent unnecessary recreation
        .withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/arm64")); // Force ARM64

    openSearchContainer.start();

    Integer mappedPort = openSearchContainer.getMappedPort(OPENSEARCH_PORT);
    String host = openSearchContainer.getHost();

    System.out.println("OpenSearch is running at: http://" + host + ":" + mappedPort);

    Runtime.getRuntime().addShutdownHook(new Thread(openSearchContainer::stop));
  }
}
