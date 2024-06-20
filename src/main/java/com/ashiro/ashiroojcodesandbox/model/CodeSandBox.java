package com.ashiro.ashiroojcodesandbox.model;

/**
 * @author ashiro
 * @description 代码沙箱接口
 */

public interface CodeSandBox {

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
