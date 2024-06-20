package com.ashiro.ashiroojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
import com.ashiro.ashiroojcodesandbox.model.ExecuteMessage;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author ashiro
 * @description
 */
@Component
public class CppDockerCodeSandBox extends CppCodeSandBoxTemplate {

    /**
     * 代码允许允许的最大时间
     * 5秒
     */
    private static final long TIME_OUT = 5L;

    /**
     * 镜像全名
     */
    private static final String IMAGE_FULL_NAME = "gcc";


    /**
     * 3、创建容器，把文件复制到容器内
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile,String classname,List<String> inputList) {
        long dockertime1 = System.currentTimeMillis();
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        // 拉取镜像，并确保镜像一定存在
        makeSureDockerImage(IMAGE_FULL_NAME, dockerClient);

        // 获取容器id，并确保容器一定存在
        String containerId = getContainerId(dockerClient, userCodeParentPath);
        System.out.println("拉取镜像获取容器消耗时间" + (System.currentTimeMillis() - dockertime1));
        // docker exec keen_blackwell java -cp /app Main 1 3
        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{classname + ".exe"}, inputArgsArray);

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();

            System.out.println("创建执行命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();

            final long[] maxMemory = {-2L};

            //获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() {
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });
            statsCmd.exec(statisticsResultCallback);
            statsCmd.start();

            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            // 判断是否超时
            // todo 利用超时标志
            final boolean[] timeout = {true};
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onError(Throwable throwable) {
                    System.out.println("错误信息" + throwable.getMessage());
                    System.out.println("错误原因" + throwable.getCause());
                    super.onError(throwable);
                }

                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    System.out.println(new String(frame.getPayload()));
                    if (StreamType.STDERR.equals(streamType)) {
                        if (errorMessage[0] == null) {
                            errorMessage[0] = new String(frame.getPayload());
                        } else {
                            errorMessage[0] += new String(frame.getPayload());
                        }
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        if (message[0] == null) {
                            message[0] = new String(frame.getPayload());
                        } else {
                            message[0] += new String(frame.getPayload());
                        }
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.SECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            }
//                catch (InterruptedException e)
//                {
//                    System.out.println("程序执行异常");
//                    throw new RuntimeException(e);
//                }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
//                    statsCmd.close();
            }
            // 获取代码运行结果
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        System.out.println("最终结果集：" + executeMessageList);

        //关闭并删除容器

        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(0).exec();
            dockerClient.removeContainerCmd(containerId).exec();
            dockerClient.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return executeMessageList;

    }

    /**
     * 确保代码沙箱镜像一定存在
     *
     * @param imageFullName
     * @param dockerClient
     */
    public void makeSureDockerImage(String imageFullName, DockerClient dockerClient) {
        if (checkImage(imageFullName, dockerClient)) {
            return;
        } else {
            downloadImage(imageFullName, dockerClient);
        }
    }

    /**
     * 检查镜像是否存在
     *
     * @param imageFullName
     * @param dockerClient
     * @return
     */
    public Boolean checkImage(String imageFullName, DockerClient dockerClient) {
        // 获取本地所有镜像
        List<Image> images = dockerClient.listImagesCmd().exec();

        // 检查指定镜像是否存在
        boolean imageExists = images.stream().anyMatch(image -> image.getRepoTags() != null && Arrays.asList(image.getRepoTags()).contains(imageFullName));

        if (imageExists) {
            System.out.println(imageFullName + "镜像存在！");
            return true;
        } else {
            System.out.println(imageFullName + "镜像不存在！");
            return false;
        }
    }

    /**
     * 下载镜像文件
     *
     * @param imageFullName
     * @param dockerClient
     */
    public void downloadImage(String imageFullName, DockerClient dockerClient) {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageFullName);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载" + imageFullName + "镜像：" + item.getStatus());
                super.onNext(item);
            }
        };
        try {
            pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            System.out.println(imageFullName + "镜像下载完成");
        } catch (InterruptedException e) {
            System.out.println("拉取" + imageFullName + "镜像异常");
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取容器id，每次提交创建一个容器
     *
     * @param dockerClient
     * @param userCodeParentPath
     * @return
     */
    public String getContainerId(DockerClient dockerClient, String userCodeParentPath) {

        System.out.println("未找到容器！");
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_FULL_NAME);
        System.out.println("容器创建成功");
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(3L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd.withHostConfig(hostConfig).withNetworkDisabled(true).withReadonlyRootfs(true).withAttachStdin(true).withAttachStderr(true).withAttachStdout(true).withTty(true).exec();
        System.out.println("创建后的容器信息：" + createContainerResponse);
        String containerId = createContainerResponse.getId();
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        System.out.println("容器启动成功");
        return containerId;

    }


}
