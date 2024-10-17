package com.server.server.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.mapper.UserMapper;
import com.server.server.request.traffic.*;
import com.server.server.service.RoadService;

public class TrafficDataConsumer implements Runnable {
    private final PriorityBlockingQueue<TrafficDataRequest> queue;
    private final TrafficDataMapper trafficDataMapper;
    private final RoadMapper roadMapper;
    private final UserMapper userMapper;  // 添加 userMapper
    private final RoadService roadService;
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults;
    private List<TrafficData> insertBatch = new ArrayList<>();
    private List<TrafficData> updateBatch = new ArrayList<>();
    private static final int BATCH_SIZE = 8; // 批量大小
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    // 非公平锁和读写锁
    private final ReentrantLock nonFairLock = new ReentrantLock(false);
    private final ReadWriteLock readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public TrafficDataConsumer(
            PriorityBlockingQueue<TrafficDataRequest> queue,
            TrafficDataMapper trafficDataMapper,
            RoadMapper roadMapper,
            UserMapper userMapper,  // 传入 userMapper
            ConcurrentHashMap<Integer, List<TrafficData>> queryResults,
            RedisTemplate<String, Object> redisTemplate,
            RoadService roadService
    ) {
        this.queue = queue;
        this.trafficDataMapper = trafficDataMapper;
        this.roadMapper = roadMapper;
        this.userMapper = userMapper;  // 初始化 userMapper
        this.queryResults = queryResults;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.roadService = roadService;

        // 初始化时加载所有道路和用户数据到 Redis
        loadInitialRoad();
        loadInitialUser();
    }

    // 在 Redis 中缓存所有道路的初始状态
    private void loadInitialRoad() {
        try {
            System.out.println("Loading initial roads into Redis...");
            List<Road> roadList = roadMapper.getAllRoads();

            for (Road road : roadList) {
                // 将每个道路的初始状态缓存到 Redis
                valueOps.set("roadData:roadId:" + road.getId(), road);
                System.out.println("Cached initial road for roadId " + road.getId());
            }
            System.out.println("Initial road data loaded into Redis successfully.");
        } catch (Exception e) {
            System.err.println("Error loading initial road data into Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 在 Redis 中缓存所有用户的初始状态
    private void loadInitialUser() {
        try {
            System.out.println("Loading initial users into Redis...");
            List<User> userList = userMapper.getAllUsers();  // 从 userMapper 获取所有用户

            for (User user : userList) {
                // 将每个用户数据缓存到 Redis
                valueOps.set("userData:userId:" + user.getId(), user);
                System.out.println("Cached initial user for userId " + user.getId());
            }
            System.out.println("Initial user data loaded into Redis successfully.");
        } catch (Exception e) {
            System.err.println("Error loading initial user data into Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Consumer started!");

        // 定期调整优先级
        scheduler.scheduleAtFixedRate(this::adjustPriorities, 0, 1, TimeUnit.SECONDS);

        // 定期更新道路状态到 Redis
        scheduler.scheduleAtFixedRate(this::checkAndUpdateAllRoadStatuses, 0, 30, TimeUnit.SECONDS);

        // 定期将 Redis 的状态持久化到数据库
        scheduler.scheduleAtFixedRate(this::updateRoadDataFromRedis, 0, 60, TimeUnit.SECONDS);

        // 处理队列中的请求
        while (true) {
            try {
                TrafficDataRequest request = queue.take();
                processRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Consumer thread interrupted: " + e.getMessage());
                break;
            }
        }
    }

    // 处理请求
    private void processRequest(TrafficDataRequest request) {
        try {
            if (request instanceof TrafficDataInsertRequest) {
                insertBatch.add(((TrafficDataInsertRequest) request).getTrafficData());
            } else if (request instanceof TrafficDataUpdateRequest) {
                updateBatch.add(((TrafficDataUpdateRequest) request).getTrafficData());
            } else if (request instanceof TrafficDataQueryRequest) {
                readWriteLock.readLock().lock(); // 获取读锁
                try {
                    List<TrafficData> data = trafficDataMapper.getTrafficDataByRoadId(((TrafficDataQueryRequest) request).getRoadId());
                    queryResults.put(((TrafficDataQueryRequest) request).getRoadId(), data);
                } finally {
                    readWriteLock.readLock().unlock(); // 释放读锁
                }
            }

            // 批量插入或更新
            if (insertBatch.size() >= BATCH_SIZE) {
                nonFairLock.lock(); // 获取非公平锁进行插入
                try {
                    trafficDataMapper.batchInsertTrafficData(insertBatch);
                    insertBatch.clear();
                } finally {
                    nonFairLock.unlock(); // 释放非公平锁
                }
            }

            if (updateBatch.size() >= BATCH_SIZE) {
                nonFairLock.lock(); // 获取非公平锁进行更新
                try {
                    trafficDataMapper.batchUpdateTrafficData(updateBatch);
                    updateBatch.clear();
                } finally {
                    nonFairLock.unlock(); // 释放非公平锁
                }
            }

        } catch (PersistenceException e) {
            System.err.println("Database error while processing request: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while processing request: " + e.getMessage());
        }
    }

    // 更新所有道路状态并缓存到 Redis
    private void checkAndUpdateAllRoadStatuses() {
        List<Road> roadList = roadService.getAllRoads();

        for (Road road : roadList) {
            String newStatus = calculateRoadStatus(trafficDataMapper.getUserCountByRoadId(road.getId()), road.getMaxLoad());

            if (!newStatus.equals(road.getStatus())) {
                nonFairLock.lock(); // 获取非公平锁进行更新
                try {
                    road.setStatus(newStatus); // 更新状态
                    valueOps.set("roadData:roadId:" + road.getId(), road); // 更新到 Redis
                    System.out.println("Updated road status for roadId " + road.getId() + ": " + newStatus);
                } finally {
                    nonFairLock.unlock(); // 释放非公平锁
                }
            }
        }
    }

    // 从 Redis 中读取道路对象并批量更新到数据库
    private void updateRoadDataFromRedis() {
        Set<String> roadKeysSet = redisTemplate.keys("roadData:roadId:*");
        if (roadKeysSet != null && !roadKeysSet.isEmpty()) {
            for (String key : roadKeysSet) {
                long roadId = Long.parseLong(key.split(":")[2]);

                readWriteLock.writeLock().lock(); // 获取写锁进行更新
                try {
                    Object roadObj = valueOps.get(key);
                    if (roadObj instanceof Road) {
                        Road roadInRedis = (Road) roadObj;
                        Road roadInDatabase = roadMapper.getRoadById(roadId);

                        if (roadInDatabase != null) {
                            boolean hasChanges = false;
                            if (!roadInRedis.getStatus().equals(roadInDatabase.getStatus())) {
                                roadMapper.updateRoadStatus(roadId, roadInRedis.getStatus());
                                hasChanges = true;
                            }
                            if (hasChanges) {
                                System.out.println("Persisted updated road data for roadId " + roadId);
                            }
                        } else {
                            roadMapper.insertRoad(roadInRedis);
                            System.out.println("Inserted new road data for roadId " + roadId);
                        }
                    } else {
                        System.err.println("Road data not found in Redis for roadId " + roadId);
                    }
                } finally {
                    readWriteLock.writeLock().unlock(); // 释放写锁
                }
            }
        }
    }

    // 调整优先级
    private void adjustPriorities() {
        long currentTime = System.currentTimeMillis();

        PriorityBlockingQueue<TrafficDataRequest> tempQueue = new PriorityBlockingQueue<>(queue.size());

        while (!queue.isEmpty()) {
            TrafficDataRequest request = queue.poll();

            if (request != null && currentTime - request.getCreatedTime() > 500) {
                request.increasePriority();
            }

            tempQueue.offer(request);
        }

        queue.addAll(tempQueue);
    }

    // 根据用户数和最大负载计算道路状态
    private String calculateRoadStatus(int userCount, int maxLoad) {
        if (userCount <= maxLoad) {
            return "绿";
        } else if (userCount <= 2 * maxLoad) {
            return "橙";
        } else {
            return "红";
        }
    }
}
