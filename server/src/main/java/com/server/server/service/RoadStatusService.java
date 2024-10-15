package com.server.server.service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RoadStatusService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    public RoadStatusService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }

    /**
     * 获取某条道路的状态
     * @param roadId 道路的唯一标识符
     * @return 道路的状态（绿、橙、红）
     */
    public String getRoadStatus(int roadId) {
        String key = "roadStatus:roadId:" + roadId;
        String status = (String) valueOps.get(key);
        System.out.println("Getting road status for roadId: " + roadId + ", Status: " + status);
        return status; // 从 Redis 中获取状态
    }
    
    public boolean isRoadStatusCached(int roadId) {
        String key = "roadStatus:roadId:" + roadId;
        boolean exists = redisTemplate.hasKey(key);
        System.out.println("Checking if road status is cached for roadId: " + roadId + ", Exists: " + exists);
        return exists; // 检查 Redis 中是否存在该键
    }
    
}
