package com.yupi.yuaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;

@SpringBootApplication(exclude = {
        // 禁用数据库自动配置（使用 MiniMax 时不需要）
        DataSourceAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class
})
public class YuAiAgentApplication {

    public static void main(String[] args) {
        // 禁用 DashScope 自动配置（使用 MiniMax 时不需要）
        System.setProperty("spring.ai.dashscope.enabled", "false");
        System.setProperty("spring.ai.dashscope.chat.enabled", "false");
        System.setProperty("spring.ai.dashscope.agent.enabled", "false");
        SpringApplication.run(YuAiAgentApplication.class, args);
    }

}
