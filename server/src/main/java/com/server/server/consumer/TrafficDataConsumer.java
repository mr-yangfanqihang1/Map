package com.server.server.consumer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import com.server.server.data.*;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.request.traffic.*;
public class TrafficDataConsumer implements Runnable {
    private final PriorityBlockingQueue<TrafficDataRequest> queue;
    private final TrafficDataMapper trafficDataMapper;
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults;

    public TrafficDataConsumer(PriorityBlockingQueue<TrafficDataRequest> queue, TrafficDataMapper trafficDataMapper, ConcurrentHashMap<Integer, List<TrafficData>> queryResults) {
        this.queue = queue;
        this.trafficDataMapper = trafficDataMapper;
        this.queryResults = queryResults;
    }

    @Override
    public void run() {
        System.out.println("Consumer started!");
        long lastPriorityAdjustmentTime = System.currentTimeMillis();
    
        while (true) {
            try {
                // 从队列中取出请求进行处理
                TrafficDataRequest request = queue.take();
                processRequest(request);
    
                // 每次处理完请求后，检查是否需要进行优先级调整
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastPriorityAdjustmentTime >= 500 && !queue.isEmpty()) { // 如果距离上次调整超过 1 秒
                    adjustPriorities();
                    lastPriorityAdjustmentTime = currentTime; // 更新上次调整时间
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Consumer thread interrupted: " + e.getMessage());
                break;
            }
        }
    }
    

    private void processRequest(TrafficDataRequest request) {
        if (request instanceof TrafficDataInsertRequest) {
            TrafficDataInsertRequest insertRequest = (TrafficDataInsertRequest) request;
            trafficDataMapper.insertTrafficData(insertRequest.getTrafficData());
            System.out.println("Processed insert request: " + insertRequest.getTrafficData());
        } else if (request instanceof TrafficDataUpdateRequest) {
            TrafficDataUpdateRequest updateRequest = (TrafficDataUpdateRequest) request;
            trafficDataMapper.updateTrafficData(updateRequest.getTrafficData());
            System.out.println("Processed update request: " + updateRequest.getTrafficData());
        } else if (request instanceof TrafficDataQueryRequest) {
            TrafficDataQueryRequest queryRequest = (TrafficDataQueryRequest) request;
            List<TrafficData> data = trafficDataMapper.getTrafficDataByRoadId(queryRequest.getRoadId());
            System.out.println("Processed query request for roadId " + queryRequest.getRoadId() + ": " + data);
            queryResults.put(queryRequest.getRoadId(), data);
        }
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