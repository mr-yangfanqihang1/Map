package com.server.server.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.Lock;

@Service
public class RoadStatusService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    // 互斥锁和读写锁
    private final ReentrantLock nonFairLock = new ReentrantLock(false); // 非公平锁
    private final ReadWriteLock readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();

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
        readWriteLock.readLock().lock(); // 获取读锁
        try {
            String status = (String) valueOps.get(key);
            System.out.println("Getting road status for roadId: " + roadId + ", Status: " + status);
            return status; // 从 Redis 中获取状态
        } finally {
            readWriteLock.readLock().unlock(); // 释放读锁
        }
    }

    /**
     * 更新某条道路的状态
     * @param roadId 道路的唯一标识符
     * @param newStatus 新的道路状态（绿、橙、红）
     */
    public void updateRoadStatus(int roadId, String newStatus) {
        String key = "roadStatus:roadId:" + roadId;
        nonFairLock.lock(); // 获取非公平锁
        try {
            valueOps.set(key, newStatus); // 更新状态到 Redis
            System.out.println("Updated road status for roadId: " + roadId + ", New Status: " + newStatus);
        } finally {
            nonFairLock.unlock(); // 释放非公平锁
        }
    }

    public boolean isRoadStatusCached(int roadId) {
        String key = "roadStatus:roadId:" + roadId;
        boolean exists = redisTemplate.hasKey(key);
        System.out.println("Checking if road status is cached for roadId: " + roadId + ", Exists: " + exists);
        return exists; // 检查 Redis 中是否存在该键
    }
}
