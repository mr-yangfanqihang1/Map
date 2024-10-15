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
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.request.traffic.*;
public class TrafficDataConsumer implements Runnable {
    private final PriorityBlockingQueue<TrafficDataRequest> queue;
    private final TrafficDataMapper trafficDataMapper;
    private final RoadMapper roadMapper;
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults;
    List<TrafficData> insertBatch = new ArrayList<>();
    List<TrafficData> updateBatch = new ArrayList<>();
    private static final int BATCH_SIZE = 8; // 定义批量操作的大小
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
        this.queryResults = queryResults;
        this.roadMapper=roadMapper;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

    }

    @Override
    public void run() {
        System.out.println("Consumer started!");
        
        // 启动优先级调整线程，每秒执行一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!queue.isEmpty()) {
                    long startTime = System.currentTimeMillis();
                    adjustPriorities();
                    long endTime = System.currentTimeMillis();
                    System.out.println("adjust time: " + (endTime - startTime) + " ms");
                }
            } catch (Exception e) {
                System.err.println("Error adjusting priorities: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);

        // 启动道路状态更新线程，每30秒执行一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndUpdateAllRoadStatuses();
            } catch (Exception e) {
                System.err.println("Error updating road statuses: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
        //持久化，每分钟一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateRoadStatusesFromRedis();
            } catch (Exception e) {
                System.err.println("Error updating road statuses from Redis: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
        // 主线程持续处理队列中的请求
        while (true) {
            try {
                // 从队列中取出请求进行处理
                TrafficDataRequest request = queue.take();
                processRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Consumer thread interrupted: " + e.getMessage());
                break;
            }
        }
    }




    private void processRequest(TrafficDataRequest request) {

        long startTime = System.currentTimeMillis();
        try {
            // 添加到对应的批量列表中
            if (request instanceof TrafficDataInsertRequest) {
                TrafficDataInsertRequest insertRequest = (TrafficDataInsertRequest) request;
                insertBatch.add(insertRequest.getTrafficData());
                System.out.println("Added to insert batch: " + insertRequest.getTrafficData()+"insertBatch.size: "+insertBatch.size());
            } else if (request instanceof TrafficDataUpdateRequest) {
                TrafficDataUpdateRequest updateRequest = (TrafficDataUpdateRequest) request;
                updateBatch.add(updateRequest.getTrafficData());
                System.out.println("Added to update batch: " + updateRequest.getTrafficData());
            } else if (request instanceof TrafficDataQueryRequest) {
                // 查询操作还是单独处理
                TrafficDataQueryRequest queryRequest = (TrafficDataQueryRequest) request;
                List<TrafficData> data = trafficDataMapper.getTrafficDataByRoadId(queryRequest.getRoadId());
                System.out.println("Processed query request for roadId " + queryRequest.getRoadId() + ": " + data);
                queryResults.put(queryRequest.getRoadId(), data);
            }

            // 当达到批量大小时，执行批量插入或更新
            if (insertBatch.size() >= BATCH_SIZE) {
                trafficDataMapper.batchInsertTrafficData(insertBatch);
                insertBatch.clear();
                System.out.println("Processed batch insert of size " + BATCH_SIZE);
            }

            if (updateBatch.size() >= BATCH_SIZE) {
                trafficDataMapper.batchUpdateTrafficData(updateBatch);
                updateBatch.clear();
                System.out.println("Processed batch update of size " + BATCH_SIZE);
            }

        } catch (PersistenceException e) {
            System.err.println("Database error while processing request: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while processing request: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("processing time: "+(endTime-startTime)+" ms");
    }



    private void checkAndUpdateAllRoadStatuses() {
        long startTime = System.currentTimeMillis();
        List<RoadTrafficData> roadTrafficDataList = trafficDataMapper.getUserCountAndMaxLoadForAllRoads();
    
        for (RoadTrafficData data : roadTrafficDataList) {
            int roadId = data.getRoadId();
            int userCount = data.getUserCount();
            int maxLoad = data.getMaxLoad();
            String status = data.getStatus();
    
            String newStatus;
            if (userCount <= maxLoad) {
                newStatus = "绿";
            } else if (userCount > maxLoad && userCount <= 2 * maxLoad) {
                newStatus = "橙";
            } else {
                newStatus = "红";
            }
    
            if (!newStatus.equals(status)) {
                // 仅更新 Redis 缓存
                valueOps.set("roadStatus:roadId:" + roadId, newStatus); 
                System.out.println("Cached road status for roadId " + roadId + ": " + newStatus);
            }
        }
    
        long endTime = System.currentTimeMillis();
        System.out.println("update road time: " + (endTime - startTime) + " ms");
    }
    // 从 Redis 中读取缓存的状态信息，并批量更新到数据库
    // 从 Redis 中读取缓存的状态信息，并批量更新到数据库
    private void updateRoadStatusesFromRedis() {
        long startTime = System.currentTimeMillis();
        Set<String> roadKeysSet = redisTemplate.keys("roadStatus:roadId:*");

        if (roadKeysSet != null && !roadKeysSet.isEmpty()) {
            List<String> roadKeys = new ArrayList<>(roadKeysSet); // 将 Set 转换为 List

            for (String key : roadKeys) {
                String roadIdStr = key.split(":")[2];
                int roadId = Integer.parseInt(roadIdStr);
                String newStatus = (String) valueOps.get(key);

                // 调用数据库更新方法
                roadMapper.updateRoadStatus(roadId, newStatus);

                // 删除已更新的 Redis 缓存
                redisTemplate.delete(key);
                System.out.println("Updated database for roadId " + roadId + " with status: " + newStatus);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("batch update road status time: " + (endTime - startTime) + " ms");
    }

    
    
    
    
    private void adjustPriorities() {
        try {
            long currentTime = System.currentTimeMillis();
            PriorityBlockingQueue<TrafficDataRequest> tempQueue = new PriorityBlockingQueue<>(queue.size());
            while (!queue.isEmpty()) {
                TrafficDataRequest request = queue.poll();
                if (request != null) {
                    long waitTime = currentTime - request.getCreatedTime();
                    if (waitTime > 500) {
                        request.increasePriority();
                        System.out.println("Increased priority for request ID: " + request.getId());
                    }
                    tempQueue.offer(request);
                }
            }
            queue.addAll(tempQueue);
        } catch (Exception e) {
            System.err.println("Error during adjusting priorities: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}