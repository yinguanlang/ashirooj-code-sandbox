package com.ashiro.ashiroojcodesandbox;

import com.ashiro.ashiroojcodesandbox.model.ExecuteCodeRequest;
import com.ashiro.ashiroojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * @author ashiro
 * @description
 */
/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate{
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
