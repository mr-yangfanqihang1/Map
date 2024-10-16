package com.server.server.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.request.traffic.*;

public class TrafficDataConsumer implements Runnable {
    private final PriorityBlockingQueue<TrafficDataRequest> queue;
    private final TrafficDataMapper trafficDataMapper;
    private final RoadMapper roadMapper;
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults;
    private List<TrafficData> insertBatch = new ArrayList<>();
    private List<TrafficData> updateBatch = new ArrayList<>();
    private static final int BATCH_SIZE = 8; // 批量大小
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;

    public TrafficDataConsumer(
        PriorityBlockingQueue<TrafficDataRequest> queue,
        TrafficDataMapper trafficDataMapper,
        RoadMapper roadMapper,
        ConcurrentHashMap<Integer, List<TrafficData>> queryResults,
        RedisTemplate<String, Object> redisTemplate
    ) {
        this.queue = queue;
        this.trafficDataMapper = trafficDataMapper;
        this.roadMapper = roadMapper;
        this.queryResults = queryResults;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        // 初始化时加载所有道路状态到 Redis
        loadInitialRoadStatuses();
    }

// 在 Redis 中缓存所有道路的初始状态
private void loadInitialRoadStatuses() {
    try {
        System.out.println("Loading initial road statuses into Redis...");
        List<RoadTrafficData> roadTrafficDataList = trafficDataMapper.getUserCountAndMaxLoadForAllRoads();
        
        for (RoadTrafficData data : roadTrafficDataList) {
            long roadId = data.getRoadId();
            String status = data.getStatus();
            
            // 将每个道路的初始状态缓存到 Redis
            valueOps.set("roadStatus:roadId:" + roadId, status);
            System.out.println("Cached initial road status for roadId " + roadId + ": " + status);
        }
        System.out.println("Initial road statuses loaded into Redis successfully.");
    } catch (Exception e) {
        System.err.println("Error loading initial road statuses into Redis: " + e.getMessage());
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
        scheduler.scheduleAtFixedRate(this::updateRoadStatusesFromRedis, 0, 60, TimeUnit.SECONDS);

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
                List<TrafficData> data = trafficDataMapper.getTrafficDataByRoadId(((TrafficDataQueryRequest) request).getRoadId());
                queryResults.put(((TrafficDataQueryRequest) request).getRoadId(), data);
            }

            // 批量插入或更新
            if (insertBatch.size() >= BATCH_SIZE) {
                trafficDataMapper.batchInsertTrafficData(insertBatch);
                insertBatch.clear();
            }

            if (updateBatch.size() >= BATCH_SIZE) {
                trafficDataMapper.batchUpdateTrafficData(updateBatch);
                updateBatch.clear();
            }

        } catch (PersistenceException e) {
            System.err.println("Database error while processing request: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while processing request: " + e.getMessage());
        }
    }

    // 更新所有道路状态并缓存到 Redis
    private void checkAndUpdateAllRoadStatuses() {
        List<RoadTrafficData> roadTrafficDataList = trafficDataMapper.getUserCountAndMaxLoadForAllRoads();
        for (RoadTrafficData data : roadTrafficDataList) {
            String newStatus = calculateRoadStatus(data.getUserCount(), data.getMaxLoad());
            if (!newStatus.equals(data.getStatus())) {
                valueOps.set("roadStatus:roadId:" + data.getRoadId(), newStatus);
            }
        }
    }

    // 从 Redis 中读取状态并批量更新到数据库
    private void updateRoadStatusesFromRedis() {
        Set<String> roadKeysSet = redisTemplate.keys("roadStatus:roadId:*");
        if (roadKeysSet != null && !roadKeysSet.isEmpty()) {
            for (String key : roadKeysSet) {
                int roadId = Integer.parseInt(key.split(":")[2]);
                String newStatus = (String) valueOps.get(key);
                roadMapper.updateRoadStatus(roadId, newStatus);
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
