package com.zhaocai.flowable;

import com.zhaocai.common.security.annotation.EnableRyFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.zhaocai.common.security.annotation.EnableCustomConfig;
import com.zhaocai.common.swagger.annotation.EnableCustomSwagger2;

/**
 * 工作流中心
 * 
 * @author Acechengui
 */
@EnableCustomConfig
@EnableCustomSwagger2
@EnableRyFeignClients
@SpringBootApplication
public class ZhaocaiFlowApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(ZhaocaiFlowApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  工作流中心启动成功   ლ(´ڡ`ლ)ﾞ ");
    }
}
