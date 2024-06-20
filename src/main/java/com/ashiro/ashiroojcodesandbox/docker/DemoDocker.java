package com.ashiro.ashiroojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * @author ashiro
 * @description
 */
public class DemoDocker {

    public static void main(String[] args) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    }
}
