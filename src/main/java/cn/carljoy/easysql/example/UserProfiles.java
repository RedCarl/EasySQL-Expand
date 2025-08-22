package cn.carljoy.easysql.example;

import cn.carljoy.easysql.annotation.*;

import java.util.Date;

/**
 * 用户档案实体类示例
 */
@Table(value = "user_profiles", comment = "用户档案表")
public class UserProfiles {

    @Id(type = "BIGINT", autoIncrement = true, comment = "主键ID")
    public Long id;

    @Column(name = "uuid", type = "VARCHAR(36)", nullable = false, unique = true, comment = "用户UUID")
    public String uuid;

    @Column(name = "username", type = "VARCHAR(50)", nullable = false, comment = "用户名")
    public String username;

    @Column(name = "email", type = "VARCHAR(100)", comment = "邮箱")
    public String email;

    @Column(name = "age", type = "INT", comment = "年龄")
    public Integer age;

    @Column(name = "status", type = "TINYINT", defaultValue = "1", comment = "状态：1-活跃，0-禁用")
    public Integer status;

    @Column(name = "created_time", type = "TIMESTAMP", defaultValue = "CURRENT_TIMESTAMP", comment = "创建时间")
    public Date createdTime;

    @Column(name = "updated_time", type = "TIMESTAMP", defaultValue = "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", comment = "更新时间")
    public Date updatedTime;

    // Getter methods for lambda expressions
    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public Integer getStatus() {
        return status;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }
}