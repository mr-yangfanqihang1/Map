package com.server.server.service;
import com.server.server.consumer.TrafficDataConsumer;
import com.server.server.data.*;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.request.traffic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Service
public class TrafficDataService {
    @Autowired
    private TrafficDataMapper trafficDataMapper;

    // 动态优先级队列

    private final PriorityBlockingQueue<TrafficDataRequest> queue = new PriorityBlockingQueue<>();
    
    // 存储查询请求结果的映射
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        System.out.println("Consumer thread starting...");
        // 启动消费者线程
        new Thread(new TrafficDataConsumer(queue, trafficDataMapper, queryResults)).start();
    }

    public void uploadTrafficData(TrafficData trafficData) {
        // 将插入请求放入队列
        TrafficDataInsertRequest insertRequest = new TrafficDataInsertRequest(trafficData);
        queue.offer(insertRequest);
        System.out.println("Insert request added to queue: " + trafficData);
    }

    public void updateTrafficData(TrafficData trafficData) {
        // 将更新请求放入队列
        TrafficDataUpdateRequest updateRequest = new TrafficDataUpdateRequest(trafficData);
        queue.offer(updateRequest);
        System.out.println("Update request added to queue: " + trafficData);
    }

    public List<TrafficData> getTrafficDataByRoadId(int roadId) {
        // 将查询请求放入队列
        TrafficDataQueryRequest queryRequest = new TrafficDataQueryRequest(roadId);
        queue.offer(queryRequest);
        System.out.println("Query request added to queue for roadId: " + roadId);
        
        // 等待查询结果
        while (!queryResults.containsKey(roadId)) {
            try {
                Thread.sleep(100); // 等待结果
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // 返回查询结果
        List<TrafficData> result = queryResults.get(roadId);
        queryResults.remove(roadId); // 清理已处理的结果
        return result;
    }
}
