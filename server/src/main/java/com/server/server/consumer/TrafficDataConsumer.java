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
        while (true) {
            try {
                TrafficDataRequest request = queue.take();
                processRequest(request);

                // 动态调整队列中请求的优先级
                adjustPriorities();
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
        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // 遍历队列中的请求
        for (TrafficDataRequest request : queue) {
            long waitTime = currentTime - request.getCreatedTime();
            if (waitTime > 5000) { // 如果等待时间超过5秒
                request.increasePriority(); // 提升优先级
                System.out.println("Increased priority for request ID: " + request.getId());
            }
        }

        // 重新构建队列以反映新的优先级
        PriorityBlockingQueue<TrafficDataRequest> tempQueue = new PriorityBlockingQueue<>(queue.size());
        tempQueue.addAll(queue);
        queue.clear();
        queue.addAll(tempQueue);
    }
}