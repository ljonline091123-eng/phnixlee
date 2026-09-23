package com.zhaocai.archives;


import com.zhaocai.common.security.annotation.EnableCustomConfig;
import com.zhaocai.common.security.annotation.EnableRyFeignClients;
import com.zhaocai.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 系统模块
 *
 * @author ruoyi
 */
@EnableCustomConfig
@EnableCustomSwagger2
@EnableRyFeignClients
@SpringBootApplication
@EnableTransactionManagement
public class ArchivesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchivesApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  档案模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              \n" +
                " 版本号时间：2025-01-01 05:20          \n"
        );
    }
}

