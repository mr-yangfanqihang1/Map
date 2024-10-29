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
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.mapper.UserMapper;
import com.server.server.request.traffic.*;
import com.server.server.service.RoadStatusWebSocketService;
import com.server.server.mapper.RouteMapper;
public class TrafficDataConsumer implements Runnable {
    private final PriorityBlockingQueue<TrafficDataRequest> queue;
    private final TrafficDataMapper trafficDataMapper;
    private final RoadMapper roadMapper;
    private final UserMapper userMapper;  // 添加 userMapper
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults;
    private List<TrafficData> insertBatch = new ArrayList<>();
    private List<TrafficData> updateBatch = new ArrayList<>();
    private static final int BATCH_SIZE = 8; // 批量大小
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(6);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;
    private RoadStatusWebSocketService roadStatusWebSocketService;

    // 非公平锁和读写锁
    private final ReentrantLock nonFairLock = new ReentrantLock(false);
    private final ReadWriteLock readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private RouteMapper routeMapper;

    public TrafficDataConsumer(
            PriorityBlockingQueue<TrafficDataRequest> queue,
            TrafficDataMapper trafficDataMapper,
            RoadMapper roadMapper,
            UserMapper userMapper,  // 传入 userMapper
            RouteMapper routeMapper,
            ConcurrentHashMap<Integer, List<TrafficData>> queryResults,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.queue = queue;
        this.trafficDataMapper = trafficDataMapper;
        this.roadMapper = roadMapper;
        this.userMapper = userMapper;  // 初始化 userMapper
        this.queryResults = queryResults;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.routeMapper = routeMapper;

        // 初始化时加载所有道路和用户数据到 Redis
        loadInitialRoad();
        loadInitialUser();
        loadInitialTrafficData();
    }

    // 在 Redis 中缓存所有道路的初始状态
    private void loadInitialRoad() {
        try {
            System.out.println("Loading initial roads into Redis...");
            List<Road> roadList = roadMapper.getAllRoads();
    
            for (Road road : roadList) {
                // 根据 distance 计算 duration（假设速度为 30 km/h，转换为分钟）
                road.setDuration(road.getDistance() * 2); // 计算时长
    
                // 将每个道路的初始状态缓存到 Redis
                valueOps.set("roadData:roadId:" + road.getId(), road);
                System.out.println("Cached initial road for roadId " + road.getId() 
                    + ", 距离: " + road.getDistance() 
                    + ", 时长: " + road.getDuration());
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

     // 在 Redis 中缓存所有trafficData的初始状态
     private void loadInitialTrafficData() {
        try {
            System.out.println("Loading initial trafficData into Redis...");
            List<TrafficData> trafficDataList = trafficDataMapper.getAllTrafficData();  // 从 trafficDataMapper 获取所有用户

            for (TrafficData trafficData : trafficDataList) {
                // 将trafficData数据缓存到 Redis
                valueOps.set("trafficData:trafficDataId:" + trafficData.getId(), trafficData);
                System.out.println("Cached initial trafficData for trafficDataId " + trafficData.getId());
            }
            System.out.println("Initial trafficData data loaded into Redis successfully.");
        } catch (Exception e) {
            System.err.println("Error loading initial trafficData data into Redis: " + e.getMessage());
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
        //定期更新user到数据库
        scheduler.scheduleAtFixedRate(this::updateUserDataFromRedis, 0, 60, TimeUnit.SECONDS);
        // 在构造函数中添加定时批处理任务
        scheduler.scheduleAtFixedRate(this::flushBatches, 0, 5, TimeUnit.SECONDS);

        // 处理队列中的请求
        while (true) {
            try {
                
                TrafficDataRequest request = queue.take();
                System.out.println(request);
                processRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Consumer thread interrupted: " + e.getMessage());
                break;
            }
        }
    }
    private void flushBatches() {
        nonFairLock.lock(); // 获取非公平锁
        try {
            if (!insertBatch.isEmpty()) {
                trafficDataMapper.batchInsertTrafficData(insertBatch);
                insertBatch.clear();
            }
            if (!updateBatch.isEmpty()) {
                trafficDataMapper.batchUpdateTrafficData(updateBatch);
                updateBatch.clear();
            }
        } finally {
            nonFairLock.unlock(); // 释放非公平锁
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
                    System.out.println("insertBatch added");
                    insertBatch.clear();
                } finally {
                    nonFairLock.unlock(); // 释放非公平锁
                }
            }

            if (updateBatch.size() >= BATCH_SIZE) {
                nonFairLock.lock(); // 获取非公平锁进行更新
                try {
                    trafficDataMapper.batchUpdateTrafficData(updateBatch);
                    System.out.println("updateBatch added");
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

    // private RoadTrafficData calculateRoadTrafficData(Road road) {
    //     // Step 1: 定义 Redis key 模式并获取所有符合条件的 trafficData 键
    //     String keyPattern = "trafficData:trafficDataId:*";
    //     System.out.println("Fetching keys with pattern: " + keyPattern);
        
    //     Set<String> keys = redisTemplate.keys(keyPattern); // 获取所有 trafficData 相关的键
    //     System.out.println("Found " + (keys != null ? keys.size() : 0) + " keys matching the pattern.");
    
    //     int userCount = 0;
    //     double speedTotal = 0.0;
    
    //     // Step 2: 如果找到符合条件的 keys，则遍历这些 keys
    //     if (keys != null && !keys.isEmpty()) {
    //         for (String key : keys) {
    //             // Step 3: 获取每个 trafficData 对象
    //             TrafficData trafficData = (TrafficData) redisTemplate.opsForValue().get(key);
                
    //             // Step 4: 检查 trafficData 是否为 null 并且 roadId 是否匹配
    //             if (trafficData != null) {
    //                 System.out.println("Processing trafficDataId: " + trafficData.getId() + " for roadId: " + trafficData.getRoadId());
    
    //                 if (trafficData.getRoadId() == road.getId()) {
    //                     userCount++; // 如果 roadId 匹配，增加用户计数
    //                     speedTotal += trafficData.getSpeed();
    //                     System.out.println("Matched roadId: " + road.getId() + ", current userCount: " + userCount + ", current speedTotal: " + speedTotal);
    //                 }
    //             } else {
    //                 System.out.println("trafficData is null for key: " + key);
    //             }
    //         }
    //     } else {
    //         System.out.println("No trafficData found for the given key pattern.");
    //     }
    
    //     // Step 5: 计算平均速度，并构造 RoadTrafficData 对象
    //     double averageSpeed = userCount > 0 ? speedTotal / userCount : 0.0;
    //     System.out.println("For roadId: " + road.getId() + ", final userCount: " + userCount + ", averageSpeed: " + averageSpeed);
    
    //     // Step 6: 创建 RoadTrafficData 对象
    //     RoadTrafficData roadTrafficData = new RoadTrafficData(road.getId(), userCount, road.getMaxLoad(), road.getStatus(), averageSpeed);
        
    //     // Step 7: 返回 RoadTrafficData 对象
    //     return roadTrafficData;
    // }
    

    

   // 更新所有道路状态并缓存到 Redis
   private void checkAndUpdateAllRoadStatuses() {
    System.out.println("Start check And Update All Road Statuses");

    List<RoadTrafficData> roadTrafficDataList = new ArrayList<>();
    
    // Step 1: 获取RoadTrafficData 对象

    try {
        roadTrafficDataList = trafficDataMapper.getRoadTrafficData();
    } catch (Exception e) {
        System.out.println("Error while getRoadTrafficData " + e.getMessage());
        e.printStackTrace();
    }

    // Step 2: 遍历每个 RoadTrafficData 进行状态检查与更新
    for (RoadTrafficData roadTrafficData : roadTrafficDataList) {
        
        Road road = (Road) redisTemplate.opsForValue().get("roadData:roadId:" + roadTrafficData.getRoadId());
        // Step 3: 计算状态
        String calculatedStatus = calculateRoadStatus(roadTrafficData.getUserCount(), roadTrafficData.getMaxLoad());
        if (!calculatedStatus.equals(roadTrafficData.getStatus())) {
            System.out.println("Road status mismatch, updating road status for roadId: " + roadTrafficData.getRoadId());
            nonFairLock.lock(); // 获取非公平锁
            try {
                if (road == null) {
                    road = new Road();
                }
                // Step 4: 更新 Road 对象属性
                road.setStatus(calculatedStatus);
                
                // Step 5: 根据计算的速度更新持续时间
                if (roadTrafficData.getAverageSpeed() != 0) {
                    //用路程除以平均速度得到预计时间
                    double newDuration=road.getDistance() * 60 / roadTrafficData.getAverageSpeed();
                    System.out.println("newDuration: " + newDuration);
                    double durationAdjustment = newDuration-road.getDuration();
                    // 查询 path 表获取包含 roadId 的用户 ID 列表
                    List<Integer> affectedUserIds = routeMapper.getUsersByRoadId(roadTrafficData.getRoadId());
                    // 通知受影响的用户
                    for (Integer userId : affectedUserIds) {
                        roadStatusWebSocketService.notifyUser(userId, roadTrafficData.getRoadId(), durationAdjustment);
                    }
                    road.setDuration(newDuration);
                } else {
                    System.out.println("Warning: Average speed is zero for roadId: " + roadTrafficData.getRoadId());
                    road.setDuration(road.getDistance()); // 防止除以零
                }
                // Step 6: 更新 Redis、数据库 中的数据
                valueOps.set("roadData:roadId:" + road.getId(), road);
                roadMapper.updateRoad(road);
                //System.out.println("Updated road status for roadId: " + road.getId());
            } catch (Exception e) {
                System.out.println("Error while updating road status for roadId: " + roadTrafficData.getRoadId() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                nonFairLock.unlock(); // 释放锁
            }
        }
    }
    System.out.println("Finished check And Update All Road Statuses");
}



    // 从 Redis 中读取道路对象并批量更新到数据库
    private void updateUserDataFromRedis() {
        System.out.println("updating UserData From redis");
        Set<String> userKeysSet = redisTemplate.keys("userData:userId:*");
        if (userKeysSet != null && !userKeysSet.isEmpty()) {
            for (String key : userKeysSet) {
                int userId = Integer.parseInt(key.split(":")[2]);
                readWriteLock.writeLock().lock(); // 获取写锁进行更新
                try {
                    Object userObj = valueOps.get(key);
                    if (userObj instanceof User) {
                        User userInRedis = (User) userObj;
                        User userInDatabase = userMapper.getUserById(userId);
                        if (userInDatabase != null) {
                            boolean hasChanges = false;
                            if (!userInRedis.getPreferences().equals(userInDatabase.getPreferences())) {
                                userMapper.updatePreferences(userId, userInRedis.getPreferences());
                                hasChanges = true;
                            }
                            if (hasChanges) {
                                System.out.println("Persisted updated user data for userId " + userId);
                            }
                        } 
                        // else {
                        //     //roadMapper.insertRoad(roadInRedis);
                        //     //System.out.println("Inserted new road data for roadId " + roadId);
                        // }//redis持久化导致的bug
                    } else {
                    //    System.err.println("Road data not found in Redis for roadId " + roadId);
                    }
                } finally {
                    readWriteLock.writeLock().unlock(); // 释放写锁
                }
            }
        }
    }


    // 从 Redis 中读取道路对象并批量更新到数据库
    private void updateRoadDataFromRedis() {
        System.out.println("updating RoadData From redis");
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
                        } 
                        // else {
                        //     //roadMapper.insertRoad(roadInRedis);
                        //     //System.out.println("Inserted new road data for roadId " + roadId);
                        // }//redis持久化导致的bug
                    } else {
                    //    System.err.println("Road data not found in Redis for roadId " + roadId);
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
