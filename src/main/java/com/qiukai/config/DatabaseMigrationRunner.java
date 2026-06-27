package com.qiukai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据库迁移：启动时检查并补充缺失的列（如 is_popular）
 */
@Configuration
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_schema = 'kazi_takeaway_project' " +
                            "AND table_name = 'dish' AND column_name = 'is_popular'",
                    Integer.class);
            if (exists != null && exists == 0) {
                jdbc.execute("ALTER TABLE dish ADD COLUMN is_popular TINYINT NOT NULL DEFAULT 0 " +
                        "COMMENT '是否人气菜品 0否 1是' AFTER is_new");
                System.out.println("[Migration] 已添加 dish.is_popular 列");
            }
        } catch (Exception e) {
            System.err.println("[Migration] 检查/添加 is_popular 列失败: " + e.getMessage());
        }
    }
}
