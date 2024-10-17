package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import java.util.Set;

    @Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    public UserService(UserMapper userMapper, RedisTemplate<String, Object> redisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }

    // 从 Redis 中获取用户的偏好
    public String getPreferences(int userId) {
        String key = "userData:userId:" + userId;
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user == null) {
            return "User not found or preferences not set.";
        }
        return user.getPreferences();
    }

    // 新增用户到 Redis
    public void createUser(User user) {
        String key = "userData:userId:" + user.getId();
        valueOps.set(key, user);
        System.out.println("User created and cached in Redis: " + user.getId());
    }

    // 更新 Redis 中的用户信息
    public void updateUser(User user) {
        String key = "userData:userId:" + user.getId();
        valueOps.set(key, user);
        System.out.println("Updated user in Redis: " + user.getId());
    }

    // 从 Redis 中删除用户
    public void deleteUser(int userId) {
        String key = "userData:userId:" + userId;
        redisTemplate.delete(key);
        System.out.println("Deleted user from Redis: " + userId);
    }

    // 从 Redis 中获取用户信息
    public User getUserById(int userId) {
        String key = "userData:userId:" + userId;
        User user = (User) valueOps.get(key);
        if (user == null) {
            System.out.println("User not found in Redis: " + userId);
        }
        return user;
    }

    // 定期将 Redis 中的用户数据更新到数据库
    public void syncRedisToDatabase() {
        // 获取 Redis 中所有的 key，假设你有方法批量获取 Redis 中的用户数据
        Set<String> keys = redisTemplate.keys("userData:userId:*");

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                User user = (User) redisTemplate.opsForValue().get(key);
                if (user != null) {
                    // 检查数据库中是否已存在该用户
                    User existingUser = userMapper.getUserById(user.getId());
                    if (existingUser == null) {
                        // 如果用户不存在于数据库中，插入数据
                        userMapper.insertUser(user);
                        System.out.println("Inserted user into database: " + user.getId());
                    } else {
                        // 如果用户已经存在，更新数据库中的用户信息
                        userMapper.updateUser(user);
                        System.out.println("Updated user in database: " + user.getId());
                    }
                }
            }
        } else {
            System.out.println("No user data found in Redis to sync.");
        }
    }
}
