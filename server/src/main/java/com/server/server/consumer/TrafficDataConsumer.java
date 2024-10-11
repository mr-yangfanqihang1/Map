package com.server.server.consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.exceptions.PersistenceException;
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
    
    public TrafficDataConsumer(PriorityBlockingQueue<TrafficDataRequest> queue, TrafficDataMapper trafficDataMapper,RoadMapper roadMapper, ConcurrentHashMap<Integer, List<TrafficData>> queryResults) {
        this.queue = queue;
        this.trafficDataMapper = trafficDataMapper;
        this.queryResults = queryResults;
        this.roadMapper=roadMapper;
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
    
        // 查询数据库，获取每个 roadId 及其用户数量、最大负载、当前状态
        List<RoadTrafficData> roadTrafficDataList = trafficDataMapper.getUserCountAndMaxLoadForAllRoads();
        // 遍历查询结果，进行状态检查和更新
        for (RoadTrafficData data : roadTrafficDataList) {
            int roadId = data.getRoadId();
            int userCount = data.getUserCount();
            int maxLoad = data.getMaxLoad();
            String status = data.getStatus();
    
            // 根据用户总量与 maxLoad 的比率设置道路状态
            String newStatus;
            if (userCount <= maxLoad) {
                newStatus = "绿";
            } else if (userCount > maxLoad && userCount <= 2 * maxLoad) {
                newStatus = "橙";
            } else {
                newStatus = "红";
            }
    
            // 如果状态有变化，更新数据库中的道路状态
            if (!newStatus.equals(status)) {
                roadMapper.updateRoadStatus(roadId, newStatus);
                System.out.println("Updated road status for roadId " + roadId + ": " + newStatus);
            }
        }
    
        long endTime = System.currentTimeMillis();
        System.out.println("update road time: " + (endTime - startTime) + " ms");
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