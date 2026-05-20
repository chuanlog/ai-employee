package org.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitConfig implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.queryForObject("SELECT ai_answer FROM sys_ticket LIMIT 1", String.class);
            System.out.println("ai_answer 字段已存在");
        } catch (Exception e) {
            System.out.println("正在添加 ai_answer 字段...");
            try {
                jdbcTemplate.execute("ALTER TABLE sys_ticket ADD COLUMN ai_answer TEXT COMMENT 'AI回答内容' AFTER question");
                System.out.println("ai_answer 字段添加成功");
            } catch (Exception ex) {
                System.out.println("添加字段时出错: " + ex.getMessage());
            }
        }
    }
}
