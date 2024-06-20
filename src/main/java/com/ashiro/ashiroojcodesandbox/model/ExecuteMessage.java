package com.ashiro.ashiroojcodesandbox.model;

import lombok.Data;


/**
 * @author ashiro
 * @description 进程执行信息
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}

