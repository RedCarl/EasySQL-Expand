package cn.carljoy.easysql.example;

import cn.carljoy.easysql.BaseDao;
import cn.carljoy.easysql.QueryWrapper;
import cc.carm.lib.easysql.api.SQLManager;

import java.util.List;

/**
 * 用户档案 DAO 示例类
 * 演示如何使用 createQuery() 方法简化 QueryWrapper 的使用
 */
public class UserProfilesDao extends BaseDao<UserProfiles> {

    public UserProfilesDao(SQLManager sm) {
        super(UserProfiles.class, sm);
    }

    /**
     * 根据 UUID 查询用户档案
     * 使用新的 createQuery() 方法，无需指定类型
     */
    public UserProfiles findByUuid(String uuid) {
        return selectOneByQuery(
            createQuery().eq(UserProfiles::getUuid, uuid)
        );
    }

    /**
     * 根据用户名模糊查询
     * 对比：之前需要写 QueryWrapper.create(UserProfiles.class)
     * 现在：直接使用 createQuery()
     */
    public List<UserProfiles> findByUsernameLike(String username) {
        return selectListByQuery(
            createQuery().like(UserProfiles::getUsername, username)
        );
    }

    /**
     * 查询活跃用户（状态为1）
     */
    public List<UserProfiles> findActiveUsers() {
        return selectListByQuery(
            createQuery().eq("status", 1)
        );
    }

    /**
     * 复杂查询示例：查询指定年龄范围内的活跃用户
     */
    public List<UserProfiles> findActiveUsersByAgeRange(int minAge, int maxAge) {
        return selectListByQuery(
            createQuery()
                .eq("status", 1)
                .between("age", minAge, maxAge)
                .orderByDesc("created_time")
        );
    }
}